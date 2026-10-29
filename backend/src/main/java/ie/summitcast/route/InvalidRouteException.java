package ie.summitcast.route;

/** The uploaded route cannot be used; the message is shown to the user. */
public class InvalidRouteException extends RuntimeException {

    public InvalidRouteException(String message) {
        super(message);
    }
}
