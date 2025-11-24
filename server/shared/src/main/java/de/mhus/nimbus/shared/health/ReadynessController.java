package de.mhus.nimbus.shared.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Zusätzlicher Endpunkt /actuator/readyness (wie angefordert) neben
 * dem Standard Actuator Pfad /actuator/health/readiness.
 */
@RestController
@RequestMapping("/actuator")
public class ReadynessController {

    private final ReadinessHealthIndicator readinessHealthIndicator;

    public ReadynessController(ReadinessHealthIndicator readinessHealthIndicator) {
        this.readinessHealthIndicator = readinessHealthIndicator;
    }

    @GetMapping("/readyness")
    public Map<String,Object> readyness() {
        var health = readinessHealthIndicator.health();
        return Map.of(
                "status", health.getStatus().getCode(),
                "readiness", health.getDetails().get("readiness")
        );
    }
}

