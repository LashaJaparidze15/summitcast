package ie.summitcast.timing;

import java.util.List;

public record TimingResult(List<TimedPoint> points, double distanceKm, double ascentM, double descentM, double totalMinutes) {
}
