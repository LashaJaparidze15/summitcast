package ie.summitcast.timing;

import ie.summitcast.geo.Geo;
import ie.summitcast.route.RoutePoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates walking time along a route.
 *
 * <ul>
 *   <li>Naismith's rule: 5 km/h on the flat, plus 1 hour for every 600 m of ascent.</li>
 *   <li>Langmuir's descent correction: on gentle descents (5 to 12 degrees) subtract 10 minutes
 *       per 300 m of descent; on steep descents (over 12 degrees) add 10 minutes per 300 m.</li>
 *   <li>A pace factor scales the result (1.0 = Naismith pace, 1.25 = 25% slower).</li>
 * </ul>
 * The rules are applied per segment between consecutive points.
 */
@Component
public class NaismithTimingModel {

    static final double FLAT_SPEED_KMH = 5.0;
    static final double ASCENT_METRES_PER_HOUR = 600.0;
    static final double DESCENT_CORRECTION_MINUTES_PER_300M = 10.0;
    static final double GENTLE_DESCENT_MIN_DEGREES = 5.0;
    static final double STEEP_DESCENT_MIN_DEGREES = 12.0;

    public TimingResult estimate(List<RoutePoint> route, double paceFactor) {
        if (paceFactor <= 0) {
            throw new IllegalArgumentException("Pace factor must be positive");
        }
        List<TimedPoint> timed = new ArrayList<>(route.size());
        double distanceKm = 0;
        double minutes = 0;
        double ascent = 0;
        double descent = 0;
        timed.add(new TimedPoint(route.get(0), 0, 0));

        for (int i = 1; i < route.size(); i++) {
            RoutePoint a = route.get(i - 1);
            RoutePoint b = route.get(i);
            double horizontalM = Geo.distanceMetres(a.lat(), a.lon(), b.lat(), b.lon());
            double climbM = b.elevation() - a.elevation();

            minutes += segmentMinutes(horizontalM, climbM) * paceFactor;
            distanceKm += horizontalM / 1000;
            if (climbM > 0) {
                ascent += climbM;
            } else {
                descent -= climbM;
            }
            timed.add(new TimedPoint(b, distanceKm, minutes));
        }
        return new TimingResult(timed, distanceKm, ascent, descent, minutes);
    }

    /** Minutes for one segment at Naismith pace, with Langmuir's descent correction. */
    static double segmentMinutes(double horizontalM, double climbM) {
        double minutes = horizontalM / 1000 / FLAT_SPEED_KMH * 60;
        if (climbM > 0) {
            minutes += climbM / ASCENT_METRES_PER_HOUR * 60;
        } else if (climbM < 0) {
            double drop = -climbM;
            double slopeDegrees = Math.toDegrees(Math.atan2(drop, horizontalM));
            double correction = drop / 300 * DESCENT_CORRECTION_MINUTES_PER_300M;
            if (slopeDegrees > STEEP_DESCENT_MIN_DEGREES) {
                minutes += correction;
            } else if (slopeDegrees >= GENTLE_DESCENT_MIN_DEGREES) {
                minutes -= correction;
            }
        }
        return minutes;
    }
}
