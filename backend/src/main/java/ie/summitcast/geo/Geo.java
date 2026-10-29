package ie.summitcast.geo;

/** Small geodesy helpers. */
public final class Geo {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private Geo() {
    }

    /** Great-circle distance in metres between two WGS84 points (haversine formula). */
    public static double distanceMetres(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = phi2 - phi1;
        double dLambda = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(h));
    }
}
