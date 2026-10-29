package ie.summitcast.plan;

import ie.summitcast.route.InvalidRouteException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

/** Turns user errors into a 400 with a message the frontend can show as is. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidRouteException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badInput(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> missing(Exception e) {
        return Map.of("error", "Please provide a GPX file and a start date and time.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badValue(MethodArgumentTypeMismatchException e) {
        return Map.of("error", "Invalid value for " + e.getName() + ".");
    }
}
