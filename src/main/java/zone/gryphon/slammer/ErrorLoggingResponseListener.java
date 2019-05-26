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

    private final LongConsumer consumer;

    private final long start;

    public ErrorLoggingResponseListener(LongConsumer consumer) {
        this.consumer = consumer;
        this.start = System.nanoTime();
    }

    @Override
    public void onComplete(Result result) {
        long duration = System.nanoTime() - start;

        try {
            if (result.isFailed()) {
                Throwable failure = result.getFailure();
                log.error("Failed to call {}", result.getRequest().getURI(), failure);
                return;
            }


            if (result.getResponse().getStatus() / 100 == 2) {
                log.trace("Successfully called URL {}", result.getRequest().getURI());
            } else {
                log.error("Status {} calling {}, response: {}",
                        result.getResponse().getStatus(), result.getRequest().getURI(), process(getContentAsString()));
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
