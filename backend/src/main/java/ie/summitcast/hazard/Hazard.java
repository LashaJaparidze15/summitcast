package ie.summitcast.hazard;

/** A triggered rule, with the forecast value that triggered it and a readable explanation. */
public record Hazard(String ruleId, String level, String metric, double value, double threshold, String explanation) {
}
