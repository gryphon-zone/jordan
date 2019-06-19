package zone.gryphon.jordan.metrics;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.Duration;

@Value
@RequiredArgsConstructor
public final class Statistics {

    private final long requests;

    private final Duration testDuration;

    private final Duration averageResponseTime;

}
