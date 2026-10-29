package ie.summitcast.forecast;

import java.util.List;

/**
 * A point forecast as returned by Met Éireann. {@code modelAltitudeM} is the terrain height of the
 * forecast model at that point, which on mountains is usually well below the real summit height.
 */
public record PointForecast(double lat, double lon, double modelAltitudeM,
                            List<ForecastHour> hours, List<PrecipitationInterval> precipitation) {
}
