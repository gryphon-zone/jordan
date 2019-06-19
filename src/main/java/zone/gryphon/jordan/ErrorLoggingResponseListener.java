/*
 * Copyright 2019-2019 Gryphon Zone
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zone.gryphon.jordan;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;

import java.net.URI;
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

            URI uri = result.getRequest().getURI();

            if (result.isFailed()) {
                log.error("request {} failed. URL: '{}'", id, uri, result.getFailure());
                return;
            }

            if (result.getResponse().getStatus() / 100 == 2) {
                log.trace("request {} completed. Successfully called URL '{}'", id, uri);
            } else {
                log.error("Request {} failed. Status {} calling '{}', response: {}",
                        id, result.getResponse().getStatus(), uri, process(getContentAsString()));
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
