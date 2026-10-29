package ie.summitcast.hazard;

/** One data-driven hazard rule, loaded from {@code hazard-rules.json}. */
public record HazardRule(String id, String metric, String operator, double threshold, String level, String message) {
}
