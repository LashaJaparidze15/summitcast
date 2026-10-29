package ie.summitcast.route;

import java.time.Instant;

/**
 * One point of a route. {@code time} is only present for recorded tracks and is kept
 * so the timing model can later be evaluated against real walks.
 */
public record RoutePoint(double lat, double lon, double elevation, Instant time) {
}
