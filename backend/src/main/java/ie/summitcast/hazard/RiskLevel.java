package ie.summitcast.hazard;

import java.util.Locale;

/** Ordered from least to most severe. */
public enum RiskLevel {
    OK, CAUTION, DANGER, UNKNOWN;

    public static RiskLevel of(String level) {
        return valueOf(level.toUpperCase(Locale.ROOT));
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The more severe of two levels. UNKNOWN (no forecast) only wins over OK. */
    public static RiskLevel worst(RiskLevel a, RiskLevel b) {
        return a.severity() >= b.severity() ? a : b;
    }

    int severity() {
        return switch (this) {
            case OK -> 0;
            case UNKNOWN -> 1;
            case CAUTION -> 2;
            case DANGER -> 3;
        };
    }
}
