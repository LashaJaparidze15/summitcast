package ie.summitcast.timing;

import ie.summitcast.route.RoutePoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class NaismithTimingModelTest {

    private final NaismithTimingModel model = new NaismithTimingModel();

    @Test
    void flatGroundIsFiveKmPerHour() {
        assertThat(NaismithTimingModel.segmentMinutes(5_000, 0)).isCloseTo(60, within(1e-9));
    }

    @Test
    void ascentAddsOneHourPer600Metres() {
        // 1 km horizontal (12 min) + 600 m climb (60 min)
        assertThat(NaismithTimingModel.segmentMinutes(1_000, 600)).isCloseTo(72, within(1e-9));
    }

    @Test
    void gentleDescentSavesTenMinutesPer300Metres() {
        // 150 m drop over 1 km is about 8.5 degrees: 12 min - 5 min
        assertThat(NaismithTimingModel.segmentMinutes(1_000, -150)).isCloseTo(7, within(1e-9));
    }

    @Test
    void steepDescentCostsTenMinutesPer300Metres() {
        // 300 m drop over 500 m is about 31 degrees: 6 min + 10 min
        assertThat(NaismithTimingModel.segmentMinutes(500, -300)).isCloseTo(16, within(1e-9));
    }

    @Test
    void veryShallowDescentHasNoCorrection() {
        // 50 m drop over 1 km is under 3 degrees
        assertThat(NaismithTimingModel.segmentMinutes(1_000, -50)).isCloseTo(12, within(1e-9));
    }

    @Test
    void correctionFlipsSignEitherSideOfTwelveDegrees() {
        double horizontal = 1_000;
        double gentleDrop = horizontal * Math.tan(Math.toRadians(11.9));
        double steepDrop = horizontal * Math.tan(Math.toRadians(12.1));

        assertThat(NaismithTimingModel.segmentMinutes(horizontal, -gentleDrop))
                .isCloseTo(12 - gentleDrop / 300 * 10, within(1e-6));
        assertThat(NaismithTimingModel.segmentMinutes(horizontal, -steepDrop))
                .isCloseTo(12 + steepDrop / 300 * 10, within(1e-6));
    }

    @Test
    void estimateAccumulatesDistanceTimeAndClimb() {
        // Three points due north, roughly 1.11 km apart: climb 300 m, then descend 100 m
        List<RoutePoint> route = List.of(
                new RoutePoint(52.00, -9.7, 100, null),
                new RoutePoint(52.01, -9.7, 400, null),
                new RoutePoint(52.02, -9.7, 300, null));

        TimingResult result = model.estimate(route, 1.0);

        assertThat(result.points()).hasSize(3);
        assertThat(result.distanceKm()).isCloseTo(2.224, within(0.01));
        assertThat(result.ascentM()).isEqualTo(300);
        assertThat(result.descentM()).isEqualTo(100);
        assertThat(result.points().getFirst().elapsedMinutes()).isZero();
        assertThat(result.points().getLast().elapsedMinutes()).isEqualTo(result.totalMinutes());
        // Elapsed time never goes backwards
        assertThat(result.points().get(1).elapsedMinutes()).isLessThan(result.totalMinutes());
    }

    @Test
    void paceFactorScalesTotalTime() {
        List<RoutePoint> route = List.of(
                new RoutePoint(52.00, -9.7, 100, null),
                new RoutePoint(52.01, -9.7, 400, null));

        double normal = model.estimate(route, 1.0).totalMinutes();
        double slower = model.estimate(route, 1.5).totalMinutes();

        assertThat(slower).isCloseTo(normal * 1.5, within(1e-9));
    }

    @Test
    void rejectsNonPositivePace() {
        List<RoutePoint> route = List.of(new RoutePoint(52, -9.7, 0, null), new RoutePoint(52.01, -9.7, 0, null));
        assertThatThrownBy(() -> model.estimate(route, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
