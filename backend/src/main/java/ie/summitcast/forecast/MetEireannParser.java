package ie.summitcast.forecast;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parses the Met Éireann point forecast XML (weatherapi 0.4 schema).
 *
 * <p>The feed mixes two kinds of {@code <time>} element: instants ({@code from == to}) carrying
 * temperature and wind, and intervals ({@code from < to}) carrying precipitation.
 */
@Component
public class MetEireannParser {

    public PointForecast parse(InputStream xml, double lat, double lon) {
        Document doc = readDocument(xml);
        List<ForecastHour> hours = new ArrayList<>();
        List<PrecipitationInterval> precipitation = new ArrayList<>();
        double modelAltitude = Double.NaN;

        NodeList times = doc.getElementsByTagName("time");
        for (int i = 0; i < times.getLength(); i++) {
            Element time = (Element) times.item(i);
            Instant from = Instant.parse(time.getAttribute("from"));
            Instant to = Instant.parse(time.getAttribute("to"));
            Element location = firstChild(time, "location");
            if (location == null) {
                continue;
            }
            if (Double.isNaN(modelAltitude) && !location.getAttribute("altitude").isEmpty()) {
                modelAltitude = Double.parseDouble(location.getAttribute("altitude"));
            }

            if (from.equals(to)) {
                Element temperature = firstChild(location, "temperature");
                Element wind = firstChild(location, "windSpeed");
                if (temperature == null || wind == null) {
                    continue;
                }
                Element gust = firstChild(location, "windGust");
                hours.add(new ForecastHour(from,
                        Double.parseDouble(temperature.getAttribute("value")),
                        Double.parseDouble(wind.getAttribute("mps")),
                        gust == null ? null : Double.parseDouble(gust.getAttribute("mps"))));
            } else {
                Element precip = firstChild(location, "precipitation");
                if (precip != null) {
                    precipitation.add(new PrecipitationInterval(from, to, Double.parseDouble(precip.getAttribute("value"))));
                }
            }
        }
        if (hours.isEmpty()) {
            throw new IllegalArgumentException("Forecast contained no hourly data");
        }
        hours.sort(Comparator.comparing(ForecastHour::time));
        return new PointForecast(lat, lon, Double.isNaN(modelAltitude) ? 0 : modelAltitude, hours, precipitation);
    }

    private static Document readDocument(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(xml);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse forecast XML", e);
        }
    }

    private static Element firstChild(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() == 0 ? null : (Element) list.item(0);
    }
}
