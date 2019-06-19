package zone.gryphon.jordan;

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
