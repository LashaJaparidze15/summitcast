package ie.summitcast.forecast;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ForecastInterpolatorTest {

    private static final Instant NINE = Instant.parse("2026-10-30T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-10-30T10:00:00Z");

    private final ForecastInterpolator interpolator = new ForecastInterpolator();

    private static PointForecast forecast(List<PrecipitationInterval> precipitation) {
        return new PointForecast(52, -9.7, 700, List.of(
                new ForecastHour(NINE, 8, 10, 15.0),
                new ForecastHour(TEN, 4, 20, 25.0)), precipitation);
    }

    @Test
    void interpolatesLinearlyBetweenHours() {
        Conditions c = interpolator.at(forecast(List.of()), Instant.parse("2026-10-30T09:15:00Z")).orElseThrow();

        assertThat(c.temperatureC()).isCloseTo(7.0, within(1e-9));
        assertThat(c.windKmh()).isCloseTo(12.5 * 3.6, within(1e-9));
        assertThat(c.gustKmh()).isCloseTo(17.5 * 3.6, within(1e-9));
        assertThat(c.modelAltitudeM()).isEqualTo(700);
    }

    @Test
    void exactForecastTimeReturnsThatHour() {
        Conditions c = interpolator.at(forecast(List.of()), TEN).orElseThrow();
        assertThat(c.temperatureC()).isEqualTo(4.0);
    }

    @Test
    void outsideTheForecastRangeIsEmpty() {
        assertThat(interpolator.at(forecast(List.of()), NINE.minusSeconds(1))).isEmpty();
        assertThat(interpolator.at(forecast(List.of()), TEN.plusSeconds(1))).isEmpty();
    }

    @Test
    void missingGustOnEitherSideGivesNoGust() {
        PointForecast f = new PointForecast(52, -9.7, 700, List.of(
                new ForecastHour(NINE, 8, 10, 15.0),
                new ForecastHour(TEN, 4, 20, null)), List.of());
        assertThat(interpolator.at(f, NINE.plusSeconds(600)).orElseThrow().gustKmh()).isNull();
    }

    @Test
    void precipitationUsesShortestIntervalAsHourlyRate() {
        List<PrecipitationInterval> rain = List.of(
                new PrecipitationInterval(NINE.minusSeconds(3600), NINE.plusSeconds(5 * 3600), 6.0),
                new PrecipitationInterval(NINE, TEN, 0.4));

        Conditions c = interpolator.at(forecast(rain), NINE.plusSeconds(1800)).orElseThrow();
        assertThat(c.precipitationMmPerHour()).isCloseTo(0.4, within(1e-9));
    }

    @Test
    void longIntervalIsConvertedToRate() {
        List<PrecipitationInterval> rain = List.of(
                new PrecipitationInterval(NINE.minusSeconds(3600), NINE.plusSeconds(5 * 3600), 6.0));

        Conditions c = interpolator.at(forecast(rain), NINE.plusSeconds(1800)).orElseThrow();
        assertThat(c.precipitationMmPerHour()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void noPrecipitationIntervalGivesNull() {
        Conditions c = interpolator.at(forecast(List.of()), NINE).orElseThrow();
        assertThat(c.precipitationMmPerHour()).isNull();
    }
}
