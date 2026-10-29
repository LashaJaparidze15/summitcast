package ie.summitcast.forecast;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Interpolates a point forecast to an exact time.
 *
 * <p>Temperature, wind and gust are interpolated linearly between the two surrounding forecast
 * instants. Precipitation is an interval total, so the shortest interval containing the time is
 * used and converted to a rate in mm per hour.
 */
@Component
public class ForecastInterpolator {

    /** Empty when the time falls outside the forecast range. */
    public Optional<Conditions> at(PointForecast forecast, Instant time) {
        List<ForecastHour> hours = forecast.hours();
        if (time.isBefore(hours.getFirst().time()) || time.isAfter(hours.getLast().time())) {
            return Optional.empty();
        }
        for (int i = 0; i < hours.size() - 1; i++) {
            ForecastHour a = hours.get(i);
            ForecastHour b = hours.get(i + 1);
            if (!time.isBefore(a.time()) && !time.isAfter(b.time())) {
                double f = fraction(a.time(), b.time(), time);
                Double gust = a.gustMps() != null && b.gustMps() != null ? lerp(a.gustMps(), b.gustMps(), f) * 3.6 : null;
                return Optional.of(new Conditions(
                        lerp(a.temperatureC(), b.temperatureC(), f),
                        lerp(a.windMps(), b.windMps(), f) * 3.6,
                        gust,
                        precipitationRate(forecast.precipitation(), time),
                        forecast.modelAltitudeM()));
            }
        }
        ForecastHour only = hours.getFirst();
        return Optional.of(new Conditions(only.temperatureC(), only.windMps() * 3.6,
                only.gustMps() == null ? null : only.gustMps() * 3.6,
                precipitationRate(forecast.precipitation(), time), forecast.modelAltitudeM()));
    }

    static double fraction(Instant from, Instant to, Instant time) {
        long span = Duration.between(from, to).toMillis();
        return span == 0 ? 0 : (double) Duration.between(from, time).toMillis() / span;
    }

    static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    static Double precipitationRate(List<PrecipitationInterval> intervals, Instant time) {
        PrecipitationInterval best = null;
        for (PrecipitationInterval p : intervals) {
            boolean contains = !time.isBefore(p.from()) && time.isBefore(p.to());
            if (contains && (best == null || Duration.between(p.from(), p.to()).compareTo(Duration.between(best.from(), best.to())) < 0)) {
                best = p;
            }
        }
        if (best == null) {
            return null;
        }
        double hours = Duration.between(best.from(), best.to()).toMinutes() / 60.0;
        return best.mm() / hours;
    }
}
