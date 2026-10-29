package ie.summitcast.forecast;

import java.time.Instant;

/** Forecast precipitation total over an interval (1 h near term, longer further out). */
public record PrecipitationInterval(Instant from, Instant to, double mm) {
}
