package ie.summitcast.forecast;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches point forecasts from Met Éireann's open data service (CC BY 4.0).
 *
 * <p>Coordinates are rounded to 0.01 degrees (about 1 km), well inside one cell of the forecast
 * model, so nearby route samples share one request. Responses are cached in memory for an hour;
 * the model only updates a few times a day.
 */
@Component
public class MetEireannClient {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final String baseUrl;
    private final MetEireannParser parser;
    private final HttpClient http;
    private final Clock clock;
    private final Map<String, CachedForecast> cache = new ConcurrentHashMap<>();

    @Autowired
    public MetEireannClient(@Value("${summitcast.met-eireann.url}") String baseUrl, MetEireannParser parser) {
        // HTTP/1.1 explicitly: the service answers 403 to Java's default HTTP/2 upgrade request.
        this(baseUrl, parser, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build(), Clock.systemUTC());
    }

    MetEireannClient(String baseUrl, MetEireannParser parser, HttpClient http, Clock clock) {
        this.baseUrl = baseUrl;
        this.parser = parser;
        this.http = http;
        this.clock = clock;
    }

    public PointForecast forecastFor(double lat, double lon) {
        double rLat = Math.round(lat * 100) / 100.0;
        double rLon = Math.round(lon * 100) / 100.0;
        String key = String.format(Locale.ROOT, "%.2f,%.2f", rLat, rLon);

        CachedForecast cached = cache.get(key);
        Instant now = clock.instant();
        if (cached != null && cached.fetchedAt().plus(CACHE_TTL).isAfter(now)) {
            return cached.forecast();
        }

        URI uri = URI.create(String.format(Locale.ROOT, "%s?lat=%.2f;long=%.2f", baseUrl, rLat, rLon));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "SummitCast/0.1 (student project)")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new ForecastUnavailableException("Met Éireann returned HTTP " + response.statusCode(), null);
            }
            try (InputStream body = response.body()) {
                PointForecast forecast = parser.parse(body, rLat, rLon);
                cache.put(key, new CachedForecast(now, forecast));
                return forecast;
            }
        } catch (ForecastUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ForecastUnavailableException("Forecast request was interrupted", e);
        } catch (Exception e) {
            throw new ForecastUnavailableException("Could not reach the Met Éireann forecast service", e);
        }
    }

    private record CachedForecast(Instant fetchedAt, PointForecast forecast) {
    }
}
