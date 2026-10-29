package ie.summitcast.hazard;

import ie.summitcast.forecast.Conditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evaluates the hazard rules against forecast conditions. Rules are data, not code: adding or
 * tuning a rule means editing {@code hazard-rules.json}.
 */
@Component
public class HazardEvaluator {

    /** Metric name used in the rules file -> label and unit used in explanations. */
    static final Map<String, String[]> METRICS = Map.of(
            "temperatureC", new String[]{"Temperature", "°C"},
            "windKmh", new String[]{"Wind", "km/h"},
            "gustKmh", new String[]{"Gusts", "km/h"},
            "precipitationMmPerHour", new String[]{"Rain", "mm/h"});

    private final List<HazardRule> rules;

    @Autowired
    public HazardEvaluator(@Value("${summitcast.hazard-rules}") Resource rulesFile) throws IOException {
        try (InputStream in = rulesFile.getInputStream()) {
            this.rules = validate(JsonMapper.builder().build().readValue(in, new TypeReference<List<HazardRule>>() {
            }));
        }
    }

    public HazardEvaluator(List<HazardRule> rules) {
        this.rules = validate(rules);
    }

    public List<HazardRule> rules() {
        return rules;
    }

    /**
     * Returns the triggered rules. When several rules on the same metric fire (say a caution and a
     * danger gust rule), only the most severe one is kept.
     */
    public List<Hazard> evaluate(Conditions conditions) {
        Map<String, Hazard> worstPerMetric = new LinkedHashMap<>();
        for (HazardRule rule : rules) {
            Double value = valueOf(conditions, rule.metric());
            if (value != null && matches(value, rule.operator(), rule.threshold())) {
                Hazard hazard = new Hazard(rule.id(), rule.level(), rule.metric(), value, rule.threshold(), explain(rule, value));
                worstPerMetric.merge(rule.metric(), hazard, (a, b) ->
                        RiskLevel.of(b.level()).severity() > RiskLevel.of(a.level()).severity() ? b : a);
            }
        }
        return new ArrayList<>(worstPerMetric.values());
    }

    public static RiskLevel levelOf(List<Hazard> hazards) {
        RiskLevel level = RiskLevel.OK;
        for (Hazard h : hazards) {
            level = RiskLevel.worst(level, RiskLevel.of(h.level()));
        }
        return level;
    }

    static Double valueOf(Conditions c, String metric) {
        return switch (metric) {
            case "temperatureC" -> c.temperatureC();
            case "windKmh" -> c.windKmh();
            case "gustKmh" -> c.gustKmh();
            case "precipitationMmPerHour" -> c.precipitationMmPerHour();
            default -> throw new IllegalArgumentException("Unknown metric " + metric);
        };
    }

    static boolean matches(double value, String operator, double threshold) {
        return switch (operator) {
            case ">=" -> value >= threshold;
            case ">" -> value > threshold;
            case "<=" -> value <= threshold;
            case "<" -> value < threshold;
            default -> throw new IllegalArgumentException("Unknown operator " + operator);
        };
    }

    static String explain(HazardRule rule, double value) {
        String[] metric = METRICS.get(rule.metric());
        return String.format(Locale.ROOT, "%s %.1f %s %s %s %s: %s (rule \"%s\")",
                metric[0], value, metric[1], rule.operator().replace(">=", "≥").replace("<=", "≤"),
                trim(rule.threshold()), metric[1], rule.message(), rule.id());
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static List<HazardRule> validate(List<HazardRule> rules) {
        for (HazardRule rule : rules) {
            if (!METRICS.containsKey(rule.metric())) {
                throw new IllegalArgumentException("Rule " + rule.id() + " uses unknown metric " + rule.metric());
            }
            matches(0, rule.operator(), 0);
            RiskLevel.of(rule.level());
        }
        return List.copyOf(rules);
    }
}
