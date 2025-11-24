package de.mhus.nimbus.shared.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Separater Endpoint /actuator/readyness (absichtliche Schreibweise wie Anforderung),
 * der den Status des ReadinessHealthIndicators spiegelt.
 */
@Component
@Endpoint(id = "readyness")
public class ReadynessEndpoint {

    private final ReadinessHealthIndicator readinessHealthIndicator;

    public ReadynessEndpoint(ReadinessHealthIndicator readinessHealthIndicator) {
        this.readinessHealthIndicator = readinessHealthIndicator;
    }

    @ReadOperation
    public Map<String, Object> read() {
        var health = readinessHealthIndicator.health();
        return Map.of(
                "status", health.getStatus().getCode(),
                "details", health.getDetails()
        );
    }
}

