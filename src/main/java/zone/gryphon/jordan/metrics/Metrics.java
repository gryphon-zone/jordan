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

package zone.gryphon.jordan.metrics;

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
