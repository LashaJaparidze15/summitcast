package ie.summitcast.hazard;

import ie.summitcast.forecast.Conditions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HazardEvaluatorTest {

    private static final List<HazardRule> RULES = List.of(
            new HazardRule("gale-gusts", "gustKmh", ">=", 75, "danger", "Gale"),
            new HazardRule("strong-gusts", "gustKmh", ">=", 50, "caution", "Gusty"),
            new HazardRule("freezing", "temperatureC", "<=", 0, "caution", "Freezing"));

    private final HazardEvaluator evaluator = new HazardEvaluator(RULES);

    private static Conditions conditions(double temp, Double gust) {
        return new Conditions(temp, 20, gust, 0.0, 500);
    }

    @Test
    void calmConditionsTriggerNothing() {
        List<Hazard> hazards = evaluator.evaluate(conditions(10, 30.0));
        assertThat(hazards).isEmpty();
        assertThat(HazardEvaluator.levelOf(hazards)).isEqualTo(RiskLevel.OK);
    }

    @Test
    void thresholdIsInclusiveForGreaterOrEqual() {
        assertThat(evaluator.evaluate(conditions(10, 50.0))).extracting(Hazard::ruleId).containsExactly("strong-gusts");
    }

    @Test
    void onlyTheMostSevereRulePerMetricIsKept() {
        List<Hazard> hazards = evaluator.evaluate(conditions(10, 80.0));
        assertThat(hazards).extracting(Hazard::ruleId).containsExactly("gale-gusts");
        assertThat(HazardEvaluator.levelOf(hazards)).isEqualTo(RiskLevel.DANGER);
    }

    @Test
    void differentMetricsAreReportedTogether() {
        List<Hazard> hazards = evaluator.evaluate(conditions(-2, 55.0));
        assertThat(hazards).extracting(Hazard::ruleId).containsExactlyInAnyOrder("strong-gusts", "freezing");
    }

    @Test
    void missingValueDoesNotTriggerItsRules() {
        assertThat(evaluator.evaluate(conditions(10, null))).isEmpty();
    }

    @Test
    void explanationShowsValueThresholdAndRule() {
        Hazard hazard = evaluator.evaluate(conditions(10, 80.4)).getFirst();
        assertThat(hazard.explanation()).isEqualTo("Gusts 80.4 km/h ≥ 75 km/h: Gale (rule \"gale-gusts\")");
        assertThat(hazard.value()).isEqualTo(80.4);
        assertThat(hazard.threshold()).isEqualTo(75);
    }

    @Test
    void rejectsUnknownMetricOperatorOrLevel() {
        assertThatThrownBy(() -> new HazardEvaluator(List.of(new HazardRule("x", "humidity", ">=", 1, "caution", ""))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HazardEvaluator(List.of(new HazardRule("x", "gustKmh", "==", 1, "caution", ""))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HazardEvaluator(List.of(new HazardRule("x", "gustKmh", ">=", 1, "scary", ""))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bundledRulesFileLoadsAndIsValid() throws Exception {
        HazardEvaluator fromFile = new HazardEvaluator(new ClassPathResource("hazard-rules.json"));
        assertThat(fromFile.rules()).isNotEmpty();
    }

    @Test
    void worstLevelOrdering() {
        assertThat(RiskLevel.worst(RiskLevel.OK, RiskLevel.UNKNOWN)).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(RiskLevel.worst(RiskLevel.UNKNOWN, RiskLevel.CAUTION)).isEqualTo(RiskLevel.CAUTION);
        assertThat(RiskLevel.worst(RiskLevel.DANGER, RiskLevel.CAUTION)).isEqualTo(RiskLevel.DANGER);
    }
}
