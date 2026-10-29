package ie.summitcast.forecast;

public class ForecastUnavailableException extends RuntimeException {

    public ForecastUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
