package ie.summitcast.plan;

import ie.summitcast.forecast.Conditions;
import ie.summitcast.forecast.ForecastInterpolator;
import ie.summitcast.forecast.ForecastUnavailableException;
import ie.summitcast.forecast.MetEireannClient;
import ie.summitcast.forecast.PointForecast;
import ie.summitcast.hazard.Hazard;
import ie.summitcast.hazard.HazardEvaluator;
import ie.summitcast.hazard.RiskLevel;
import ie.summitcast.route.GpxRouteParser;
import ie.summitcast.route.RoutePoint;
import ie.summitcast.timing.NaismithTimingModel;
import ie.summitcast.timing.TimedPoint;
import ie.summitcast.timing.TimingResult;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds a trip plan: parse the route, estimate arrival times, sample the forecast along the
 * route, interpolate it to each arrival time and evaluate the hazard rules.
 */
@Service
public class PlanService {

    static final ZoneId IRELAND = ZoneId.of("Europe/Dublin");
    /** Forecast samples are taken roughly this far apart along the route. */
    static final double SAMPLE_SPACING_KM = 1.0;
    static final int MAX_SAMPLES = 25;
    static final String SOURCE = "Met Éireann open data (CC BY 4.0)";

    private final GpxRouteParser parser;
    private final NaismithTimingModel timingModel;
    private final MetEireannClient forecastClient;
    private final ForecastInterpolator interpolator;
    private final HazardEvaluator hazards;

    public PlanService(GpxRouteParser parser, NaismithTimingModel timingModel, MetEireannClient forecastClient,
                       ForecastInterpolator interpolator, HazardEvaluator hazards) {
        this.parser = parser;
        this.timingModel = timingModel;
        this.forecastClient = forecastClient;
        this.interpolator = interpolator;
        this.hazards = hazards;
    }

    public PlanResponse plan(InputStream gpx, LocalDateTime localStart, double paceFactor) {
        List<RoutePoint> route = parser.parse(gpx);
        TimingResult timing = timingModel.estimate(route, paceFactor);
        Instant start = localStart.atZone(IRELAND).toInstant();

        List<Integer> sampleIndexes = sampleIndexes(timing);
        List<PointForecast> forecasts = new ArrayList<>();
        String outage = null;
        try {
            for (int index : sampleIndexes) {
                RoutePoint p = timing.points().get(index).point();
                forecasts.add(forecastClient.forecastFor(p.lat(), p.lon()));
            }
        } catch (ForecastUnavailableException e) {
            outage = e.getMessage();
            forecasts.clear();
        }

        List<PlanResponse.PlanPoint> points = new ArrayList<>();
        RiskLevel overall = RiskLevel.OK;
        int outOfRange = 0;
        int sample = 0;
        for (int i = 0; i < timing.points().size(); i++) {
            TimedPoint tp = timing.points().get(i);
            Instant arrival = start.plusSeconds(Math.round(tp.elapsedMinutes() * 60));

            Conditions conditions = null;
            if (!forecasts.isEmpty()) {
                sample = nearestSample(timing, sampleIndexes, i, sample);
                Optional<Conditions> found = interpolator.at(forecasts.get(sample), arrival);
                if (found.isPresent()) {
                    conditions = rounded(found.get());
                } else {
                    outOfRange++;
                }
            }
            List<Hazard> triggered = conditions == null ? List.of() : hazards.evaluate(conditions);
            RiskLevel level = conditions == null ? RiskLevel.UNKNOWN : HazardEvaluator.levelOf(triggered);
            overall = RiskLevel.worst(overall, level);

            RoutePoint p = tp.point();
            points.add(new PlanResponse.PlanPoint(p.lat(), p.lon(), round(p.elevation(), 0), round(tp.distanceKm(), 3),
                    arrival.atZone(IRELAND).toOffsetDateTime(), conditions, level.key(), triggered));
        }

        Instant finish = start.plusSeconds(Math.round(timing.totalMinutes() * 60));
        PlanResponse.Summary summary = new PlanResponse.Summary(round(timing.distanceKm(), 2), round(timing.ascentM(), 0),
                round(timing.descentM(), 0), round(timing.totalMinutes(), 0), start.atZone(IRELAND).toOffsetDateTime(),
                finish.atZone(IRELAND).toOffsetDateTime(), paceFactor, overall.key());
        return new PlanResponse(summary, forecastStatus(outage, outOfRange, points.size(), forecasts), points);
    }

    private static PlanResponse.ForecastStatus forecastStatus(String outage, int outOfRange, int total,
                                                              List<PointForecast> forecasts) {
        if (outage != null) {
            return new PlanResponse.ForecastStatus(false,
                    outage + ". Timings are still shown, but no weather or hazards could be checked.", 0, SOURCE);
        }
        String message;
        if (outOfRange == total) {
            message = "The whole walk is outside the forecast range (forecasts run until "
                    + forecasts.getFirst().hours().getLast().time().atZone(IRELAND).toLocalDateTime() + "). Pick a start time within it.";
        } else if (outOfRange > 0) {
            message = "Part of the walk is outside the forecast range, so those sections show no conditions.";
        } else {
            message = "Forecast covers the whole walk.";
        }
        return new PlanResponse.ForecastStatus(true, message, forecasts.size(), SOURCE);
    }

    /** Indexes of route points where the forecast is sampled: the start, then every ~1 km, and the end. */
    static List<Integer> sampleIndexes(TimingResult timing) {
        List<TimedPoint> pts = timing.points();
        double spacing = Math.max(SAMPLE_SPACING_KM, timing.distanceKm() / (MAX_SAMPLES - 1));
        List<Integer> indexes = new ArrayList<>();
        indexes.add(0);
        double next = spacing;
        for (int i = 1; i < pts.size() - 1; i++) {
            if (pts.get(i).distanceKm() >= next) {
                indexes.add(i);
                next += spacing;
            }
        }
        indexes.add(pts.size() - 1);
        return indexes;
    }

    /** The sample nearest by distance along the route. Samples are in route order, so we only walk forward. */
    private static int nearestSample(TimingResult timing, List<Integer> sampleIndexes, int pointIndex, int current) {
        double d = timing.points().get(pointIndex).distanceKm();
        while (current + 1 < sampleIndexes.size()) {
            double here = Math.abs(timing.points().get(sampleIndexes.get(current)).distanceKm() - d);
            double next = Math.abs(timing.points().get(sampleIndexes.get(current + 1)).distanceKm() - d);
            if (next <= here) {
                current++;
            } else {
                break;
            }
        }
        return current;
    }

    private static Conditions rounded(Conditions c) {
        return new Conditions(round(c.temperatureC(), 1), round(c.windKmh(), 1),
                c.gustKmh() == null ? null : round(c.gustKmh(), 1),
                c.precipitationMmPerHour() == null ? null : round(c.precipitationMmPerHour(), 1),
                c.modelAltitudeM());
    }

    private static double round(double v, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(v * scale) / scale;
    }
}
