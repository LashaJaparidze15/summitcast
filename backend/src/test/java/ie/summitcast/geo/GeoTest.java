package ie.summitcast.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoTest {

    @Test
    void samePointIsZeroDistance() {
        assertThat(Geo.distanceMetres(52.0, -9.7, 52.0, -9.7)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertThat(Geo.distanceMetres(52.0, -9.7, 53.0, -9.7)).isCloseTo(111_195, within(10.0));
    }

    @Test
    void agreesWithFlatEarthApproximationOverShortDistances() {
        // Over a few km the equirectangular approximation is accurate to well under 0.1%
        double lat1 = 52.0300, lon1 = -9.7000, lat2 = 51.9993, lon2 = -9.7425;
        double metresPerDegree = 6_371_000 * Math.PI / 180;
        double dy = (lat2 - lat1) * metresPerDegree;
        double dx = (lon2 - lon1) * metresPerDegree * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        double approx = Math.hypot(dx, dy);

        assertThat(Geo.distanceMetres(lat1, lon1, lat2, lon2)).isCloseTo(approx, within(approx * 0.001));
    }
}
