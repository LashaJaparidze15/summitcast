package ie.summitcast.forecast;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MetEireannParserTest {

    @Test
    void parsesInstantsIntervalsAndModelAltitude() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/met-eireann-sample.xml")) {
            PointForecast f = new MetEireannParser().parse(in, 52.0, -9.74);

            assertThat(f.modelAltitudeM()).isEqualTo(759);
            assertThat(f.hours()).hasSize(3);
            ForecastHour first = f.hours().getFirst();
            assertThat(first.time()).isEqualTo(Instant.parse("2026-10-30T09:00:00Z"));
            assertThat(first.temperatureC()).isEqualTo(8.0);
            assertThat(first.windMps()).isEqualTo(10.0);
            assertThat(first.gustMps()).isEqualTo(15.0);
            assertThat(f.hours().getLast().gustMps()).as("gust missing in the feed").isNull();

            assertThat(f.precipitation()).hasSize(3);
            assertThat(f.precipitation().getFirst().mm()).isEqualTo(0.4);
        }
    }
}
