package ie.summitcast.timing;

import ie.summitcast.route.RoutePoint;

/** A route point with its cumulative distance and estimated walking time from the start. */
public record TimedPoint(RoutePoint point, double distanceKm, double elapsedMinutes) {
}
