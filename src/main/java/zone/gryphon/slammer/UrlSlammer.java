package zone.gryphon.slammer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author galen
 */
public class UrlSlammer {

    private static final Logger log = LoggerFactory.getLogger(UrlSlammer.class);

    private static final int cores = Runtime.getRuntime().availableProcessors();

    public static void main(String... args) {
        new UrlSlammer(args).run();
    }

    @Parameter(
            names = "--params",
            converter = RequiredFileConverter.class,
            description = "" +
                    "CSV file containing parameters for making requests. " +
                    "The file must contain a header row, which names the parameter values in each column represent."
    )
    private File paramsFile;

    /**
     *
     */
    @Parameter(
            required = true,
            names = "--url",
            description = "" +
                    "The URL to hit. " +
                    "When --paramsFile is specified, the url can contain substitution parameters using the syntax {key}, e.g. " +
                    "http://example.com/page/{pageNumber}"
    )
    private String url;

    /**
     * HTTP method to use when making requests
     */
    @Parameter(
            names = {"-X", "--method"},
            description = "" +
                    "The HTTP method to use when issuing requests"
    )
    private String httpMethod = "GET";

    @Parameter(
            names = {"-r", "--rate"},
            description = "" +
                    "The maximum rate to issue requests at, in units of requests/second. " +
                    "Note that the actual rate may be lower than this value, " +
                    "if maintaining the requested rate would require breaching the configured maximum number of concurrent requests"
    )
    private double rate = 5000;


    @Parameter(
            names = {"--max-concurrent-requests"},
            description = "" +
                    "The maximum number of outstanding requests to have open at any point in time. " +
                    "Having too many requests open simultaneously can cause port exhaustion, leading to failed requests."
    )
    private int maxActiveRequests = 1000;

    @Parameter(
            names = {"-h", "--help"},
            help = true,
            description = "Print this help message and exit"
    )
    private boolean printHelp = false;

    private final Object mutex = new Object();

    private final Pattern pattern = Pattern.compile("\\{([\\w|\\s]+)}");

    private final HttpClient client;

    private final AtomicLong requestsInflight = new AtomicLong(0);

    private final AtomicLong responseTimeNanoSum = new AtomicLong(0);

    private final ExecutorService executorService;

    private UrlSlammer(String... args) {

        try {
            JCommander.newBuilder()
                    .addObject(this)
                    .acceptUnknownOptions(true)
                    .build()
                    .parse(args);
        } catch (Exception e) {
            log.error("Failed to parse arguments", e);
            System.exit(1);
        }

        if (printHelp) {
            JCommander.newBuilder()
                    .addObject(this)
                    .build()
                    .usage();
            System.exit(0);
        }

        client = new HttpClient(new SslContextFactory());
        executorService = Executors.newFixedThreadPool(cores * 2);
    }

    private void run() {
        try {
            client.setConnectBlocking(false);
            client.setTCPNoDelay(false);

            client.setConnectTimeout(Duration.ofSeconds(90).toMillis());
            client.setAddressResolutionTimeout(Duration.ofSeconds(90).toMillis());
            client.setStrictEventOrdering(false);

            client.setExecutor(executorService);

            client.setMaxConnectionsPerDestination(512);

            client.setMaxRequestsQueuedPerDestination(Short.MAX_VALUE);

            client.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start HTTP client", e);
        }

        try {
            log.info("About to start slamming URLs");
            Statistics statistics = doSlam();

            double durationInSeconds = statistics.getTestDuration().toNanos() / 1000000000.0;
            double averageResponseTimeMs = statistics.getAverageResponseTime().toNanos() / 1000000.0;

            String time = String.format("%.3f", durationInSeconds);
            String rate = String.format("%.3f", statistics.getRequests() / durationInSeconds);
            String responseTime = String.format("%.3f", averageResponseTimeMs);

            log.info("Done slamming URLs. Hit {} URLs in {} seconds; avg {} requests/sec, average response time {} ms",
                    statistics.getRequests(), time, rate, responseTime);
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                log.warn("Failed to close HTTP client cleanly", e);
            }
        }
    }

    private Iterable<Request> calculateUrls() {
        Map<String, String> params = new HashMap<>();

        Matcher matcher = pattern.matcher(url);

        while (matcher.find()) {
            params.put(matcher.group(1), String.format("{%s}", matcher.group(1)));
        }

        if (params.isEmpty()) {
            // no params, and a single URL means we only have 1 request
            return Collections.singleton(client.newRequest(url).method(httpMethod));
        } else {
            if (paramsFile == null) {
                log.error("Parameter file must be provided when parameter substitution is used in URL or body");
                System.exit(1);
            }

            try {
                BufferedReader isr = new BufferedReader(new InputStreamReader(new FileInputStream(paramsFile)));

                CSVParser csvParser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(isr);

                return () -> new Iterator<Request>() {

                    final Iterator<CSVRecord> wrapped = csvParser.iterator();

                    @Override
                    public boolean hasNext() {
                        return wrapped.hasNext();
                    }

                    @Override
                    public Request next() {
                        CSVRecord record = wrapped.next();

                        StringBuilder builder = new StringBuilder(url);

                        params.forEach((name, substitution) -> {
                            int startIndex;

                            while ((startIndex = builder.indexOf(substitution)) >= 0) {
                                builder.replace(startIndex, startIndex + substitution.length(), record.get(name));
                            }
                        });

                        return client.newRequest(builder.toString())
                                .method(httpMethod);
                    }
                };
            } catch (IOException e) {
                throw new RuntimeException("Failed to read in file " + paramsFile.getAbsolutePath(), e);
            }
        }
    }

    private Request applyReadTimeout(Request request) {
        return request;

    }

    private Statistics doSlam() {
        long start = System.nanoTime();

        Meter meter = new Meter();

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("Rate: {} requests/second, requests: {}", String.format("%.2f", meter.rate()), meter.count());
            }
        }, 1000, 1000);

        for (Request request : calculateUrls()) {

            // ensure we aren't breaching the desired request rate
            while (meter.rate() > rate) {

                // the expected test duration based on the desired rate and number of requests
                long expectedDurationInNanoseconds = (long) ((meter.count() / rate) * 1_000_000_000);

                // how long the test has been running
                long durationInNanoSeconds = System.nanoTime() - start;

                long deltaInNanoseconds = expectedDurationInNanoseconds - durationInNanoSeconds;

                if (deltaInNanoseconds > 0) {
                    long deltaInMilliseconds = (long) Math.ceil(deltaInNanoseconds / 1_0000_000.0);

                    sleep(deltaInMilliseconds);
                }
            }

            while (requestsInflight.get() > maxActiveRequests) {
                sleep(10);
            }


            requestsInflight.incrementAndGet();
            meter.mark();
            applyReadTimeout(request).send(new ErrorLoggingResponseListener(this::requestCompleted));
        }

        long complete = System.nanoTime();
        log.info("Done issuing requests (issued {} requests)", meter.count());

        // block until all the requests have completed
        long inFlight;

        while ((inFlight = requestsInflight.get()) > 0) {
            log.info("Waiting for {} requests to complete", inFlight);
            synchronized (mutex) {
                try {
                    mutex.wait(10000);
                } catch (InterruptedException e) {
                    log.debug("InterruptedException waiting for mutex to complete", e);
                }
            }
        }

        timer.cancel();
        executorService.shutdown();
        return new Statistics(meter.count(), Duration.ofNanos(complete - start), Duration.ofNanos(responseTimeNanoSum.get() / meter.count()));
    }

    private void requestCompleted(long duration) {
        responseTimeNanoSum.addAndGet(duration);

        long remainingInFlight = requestsInflight.decrementAndGet();

        if (remainingInFlight <= 0) {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }
    }

    private static void sleep(long duration) {
        if (duration <= 0) {
            return;
        }

        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        }
    }
}
