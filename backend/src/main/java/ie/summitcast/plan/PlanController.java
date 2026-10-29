package ie.summitcast.plan;

import ie.summitcast.hazard.HazardEvaluator;
import ie.summitcast.hazard.HazardRule;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PlanController {

    static final double MIN_PACE = 0.5;
    static final double MAX_PACE = 2.5;

    private final PlanService planService;
    private final HazardEvaluator hazardEvaluator;

    public PlanController(PlanService planService, HazardEvaluator hazardEvaluator) {
        this.planService = planService;
        this.hazardEvaluator = hazardEvaluator;
    }

    /**
     * Plans a walk.
     *
     * @param file       GPX track or route
     * @param start      planned start, local Irish time, e.g. 2026-10-30T09:00
     * @param paceFactor 1.0 = Naismith pace, 1.25 = 25% slower
     */
    @PostMapping(value = "/plan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlanResponse plan(@RequestParam MultipartFile file,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                             @RequestParam(defaultValue = "1.0") double paceFactor) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a GPX file.");
        }
        if (paceFactor < MIN_PACE || paceFactor > MAX_PACE) {
            throw new IllegalArgumentException("Pace factor must be between " + MIN_PACE + " and " + MAX_PACE + ".");
        }
        try (InputStream in = file.getInputStream()) {
            return planService.plan(in, start, paceFactor);
        }
    }

    /** The hazard rules in use, so the UI can show exactly what is being checked. */
    @GetMapping("/rules")
    public List<HazardRule> rules() {
        return hazardEvaluator.rules();
    }
}
