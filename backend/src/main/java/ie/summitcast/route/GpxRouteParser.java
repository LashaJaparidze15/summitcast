package ie.summitcast.route;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Length;
import io.jenetics.jpx.Route;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Reads a GPX file (track or route) into a list of points, using the jpx library. */
@Component
public class GpxRouteParser {

    public List<RoutePoint> parse(InputStream in) {
        GPX gpx;
        try {
            gpx = GPX.Reader.DEFAULT.read(in);
        } catch (Exception e) {
            throw new InvalidRouteException("Could not read the file as GPX.");
        }

        List<WayPoint> wayPoints = gpx.tracks()
                .flatMap(Track::segments)
                .flatMap(TrackSegment::points)
                .toList();
        if (wayPoints.isEmpty()) {
            wayPoints = gpx.routes().flatMap(Route::points).toList();
        }
        if (wayPoints.size() < 2) {
            throw new InvalidRouteException("The GPX file needs a track or route with at least two points.");
        }

        Double[] elevations = new Double[wayPoints.size()];
        for (int i = 0; i < wayPoints.size(); i++) {
            elevations[i] = wayPoints.get(i).getElevation().map(Length::doubleValue).orElse(null);
        }
        fillElevationGaps(elevations);

        List<RoutePoint> points = new ArrayList<>(wayPoints.size());
        for (int i = 0; i < wayPoints.size(); i++) {
            WayPoint wp = wayPoints.get(i);
            Instant time = wp.getTime().orElse(null);
            points.add(new RoutePoint(wp.getLatitude().doubleValue(), wp.getLongitude().doubleValue(), elevations[i], time));
        }
        return points;
    }

    /**
     * Fills missing elevations by linear interpolation between the nearest known neighbours.
     * A file with no elevation at all is rejected for now; a terrain model lookup will handle that case.
     */
    static void fillElevationGaps(Double[] elevations) {
        int firstKnown = -1;
        for (int i = 0; i < elevations.length; i++) {
            if (elevations[i] != null) {
                firstKnown = i;
                break;
            }
        }
        if (firstKnown == -1) {
            throw new InvalidRouteException("This GPX file has no elevation data. Elevation lookup from a terrain model is planned for the next iteration.");
        }
        for (int i = 0; i < firstKnown; i++) {
            elevations[i] = elevations[firstKnown];
        }
        int prev = firstKnown;
        for (int i = firstKnown + 1; i < elevations.length; i++) {
            if (elevations[i] == null) {
                continue;
            }
            for (int j = prev + 1; j < i; j++) {
                double f = (double) (j - prev) / (i - prev);
                elevations[j] = elevations[prev] + f * (elevations[i] - elevations[prev]);
            }
            prev = i;
        }
        for (int i = prev + 1; i < elevations.length; i++) {
            elevations[i] = elevations[prev];
        }
    }
}
