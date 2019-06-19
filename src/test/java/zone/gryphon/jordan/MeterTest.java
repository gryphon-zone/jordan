package zone.gryphon.jordan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import zone.gryphon.jordan.metrics.Meter;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class MeterTest {

    @Test
    public void name() {
        assertThat(new Meter().count()).isEqualTo(0L);
    }
}