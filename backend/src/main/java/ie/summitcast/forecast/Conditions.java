package ie.summitcast.forecast;

/** Forecast conditions at one place and time, after interpolation. */
public record Conditions(double temperatureC, double windKmh, Double gustKmh,
                         Double precipitationMmPerHour, double modelAltitudeM) {
}
