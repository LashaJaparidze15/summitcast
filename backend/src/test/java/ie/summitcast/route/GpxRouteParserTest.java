package ie.summitcast.route;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GpxRouteParserTest {

    private final GpxRouteParser parser = new GpxRouteParser();

    private static InputStream gpx(String body) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">%s</gpx>
                """.formatted(body);
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsTrackPointsWithElevationAndTime() {
        List<RoutePoint> points = parser.parse(gpx("""
                <trk><trkseg>
                  <trkpt lat="52.03" lon="-9.70"><ele>118</ele><time>2026-10-30T09:00:00Z</time></trkpt>
                  <trkpt lat="52.02" lon="-9.71"><ele>250</ele><time>2026-10-30T09:20:00Z</time></trkpt>
                </trkseg></trk>"""));

        assertThat(points).hasSize(2);
        assertThat(points.get(0).lat()).isEqualTo(52.03);
        assertThat(points.get(1).elevation()).isEqualTo(250);
        assertThat(points.get(1).time()).isEqualTo(Instant.parse("2026-10-30T09:20:00Z"));
    }

    @Test
    void fallsBackToRoutePointsWhenThereIsNoTrack() {
        List<RoutePoint> points = parser.parse(gpx("""
                <rte>
                  <rtept lat="52.03" lon="-9.70"><ele>118</ele></rtept>
                  <rtept lat="52.02" lon="-9.71"><ele>250</ele></rtept>
                </rte>"""));

        assertThat(points).hasSize(2);
        assertThat(points.getFirst().time()).isNull();
    }

    @Test
    void interpolatesMissingElevations() {
        Double[] elevations = {null, 100.0, null, null, 400.0, null};
        GpxRouteParser.fillElevationGaps(elevations);
        assertThat(elevations).containsExactly(100.0, 100.0, 200.0, 300.0, 400.0, 400.0);
    }

    @Test
    void rejectsFileWithNoElevation() {
        assertThatThrownBy(() -> parser.parse(gpx("""
                <trk><trkseg>
                  <trkpt lat="52.03" lon="-9.70"/>
                  <trkpt lat="52.02" lon="-9.71"/>
                </trkseg></trk>""")))
                .isInstanceOf(InvalidRouteException.class)
                .hasMessageContaining("no elevation");
    }

    @Test
    void rejectsSinglePointRoute() {
        assertThatThrownBy(() -> parser.parse(gpx("""
                <trk><trkseg><trkpt lat="52.03" lon="-9.70"><ele>1</ele></trkpt></trkseg></trk>""")))
                .isInstanceOf(InvalidRouteException.class);
    }

    @Test
    void rejectsSomethingThatIsNotGpx() {
        InputStream junk = new ByteArrayInputStream("not xml at all".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> parser.parse(junk)).isInstanceOf(InvalidRouteException.class);
    }

    @Test
    void parsesTheBundledSampleRoute() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/carrauntoohil-devils-ladder.gpx")) {
            List<RoutePoint> points = parser.parse(in);
            assertThat(points).hasSizeGreaterThan(100);
            assertThat(points.stream().mapToDouble(RoutePoint::elevation).max().orElseThrow()).isGreaterThan(1000);
        }
    }
}
