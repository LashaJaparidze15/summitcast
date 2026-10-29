package ie.summitcast.forecast;

import java.time.Instant;

/** Instantaneous forecast values at one time. Gust can be missing in the longer-range model. */
public record ForecastHour(Instant time, double temperatureC, double windMps, Double gustMps) {
}
