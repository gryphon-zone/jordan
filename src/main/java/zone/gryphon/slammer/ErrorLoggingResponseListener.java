package zone.gryphon.slammer;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;

import java.util.function.LongConsumer;

/**
 * @author galen
 */

@Slf4j
public class ErrorLoggingResponseListener extends BufferingResponseListener {

    private long id;
    private final LongConsumer consumer;

    private final long start;

    public ErrorLoggingResponseListener(long id, LongConsumer consumer) {
        this.id = id;
        this.consumer = consumer;
        this.start = System.nanoTime();
    }

    @Override
    public void onComplete(Result result) {
        long duration = System.nanoTime() - start;

        try {
            if (result.isFailed()) {
                Throwable failure = result.getFailure();
//                log.error("request {} failed. URL: '{}': {}: {}", id, result.getRequest().getURI(), failure.getClass().getSimpleName(), failure.getLocalizedMessage(), failure);
                log.error("request {} failed. URL: '{}'", id, result.getRequest().getURI(), failure);
                System.exit(1);
                return;

            }


            if (result.getResponse().getStatus() / 100 == 2) {
                log.trace("request {} completed. Successfully called URL {}", id, result.getRequest().getURI());
            } else {
                log.error("Request {} failed. Status {} calling {}, response: {}",
                        id, result.getResponse().getStatus(), result.getRequest().getURI(), process(getContentAsString()));
            }

        } finally {
            try {
                consumer.accept(duration);
            } catch (Exception e) {
                log.debug("consumer threw exception", e);
            }
        }

    }

    private String process(String response) {

        if (response == null || response.isEmpty()) {
            return "<no response from server>";
        }

        response = response.replaceAll("[\r\n]", " ");

        return response.length() <= 512 ? response : response.substring(0, 509) + "...";

    }

}
