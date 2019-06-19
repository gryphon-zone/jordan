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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Meter {

    private final long start = System.nanoTime();

    private final AtomicLong requests = new AtomicLong(0);


    public long count() {
        return requests.get();
    }

    public void mark() {
        requests.getAndIncrement();
    }

    public long duration(TimeUnit timeUnit) {
        return timeUnit.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }

    /**
     * Return the current rate, in requests/second
     *
     * @return The rate
     */
    public double rate() {

        if (requests.get() == 0) {
            return 0;
        }

        long duration = System.nanoTime() - start;
        return (((double) requests.get()) / duration) * 1_000_000_000;
    }

}
