package ie.summitcast.plan;

import ie.summitcast.forecast.Conditions;
import ie.summitcast.hazard.Hazard;

import java.time.OffsetDateTime;
import java.util.List;

public record PlanResponse(Summary summary, ForecastStatus forecast, List<PlanPoint> points) {

    public record Summary(double distanceKm, double ascentM, double descentM, double durationMinutes,
                          OffsetDateTime start, OffsetDateTime finish, double paceFactor, String overallLevel) {
    }

    /** {@code available} is false when the forecast service could not be reached at all. */
    public record ForecastStatus(boolean available, String message, int locationsQueried, String source) {
    }

    /** {@code conditions} is null when no forecast covers this point's arrival time. */
    public record PlanPoint(double lat, double lon, double elevationM, double distanceKm, OffsetDateTime arrival,
                            Conditions conditions, String level, List<Hazard> hazards) {
    }
}
