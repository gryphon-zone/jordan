package zone.gryphon.jordan;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class Metrics implements Closeable {

    private static final long oneSecondInMs = Duration.ofSeconds(1).toMillis();

    private final Timer timer = new Timer("statistics");

    public Metrics(@NonNull Meter meter, @NonNull Meter started) {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                log.info("Average request rate: {} requests/second. Total requests issued: {}, total requests queued: {}",
                        String.format("%.3f", started.rate()), started.count(), meter.count());
            }

        }, oneSecondInMs, oneSecondInMs);
    }

    @Override
    public void close() {
        try {
            timer.cancel();
        } catch (Exception e) {
            // ignore
        }
    }

}
