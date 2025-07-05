package de.mhus.nimbus.common.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility-Klasse für Request-ID Generierung und -Validierung
 * Folgt Spring Boot Naming Conventions
 */
@Component
public class RequestIdUtils {

    private static final String REQUEST_ID_PREFIX = "nimbus-";

    /**
     * Generiert eine neue Request-ID
     */
    public String generateRequestId() {
        return REQUEST_ID_PREFIX + UUID.randomUUID().toString();
    }

    /**
     * Generiert eine Request-ID mit Service-Präfix
     */
    public String generateRequestId(String serviceName) {
        return REQUEST_ID_PREFIX + serviceName + "-" + UUID.randomUUID().toString();
    }

    /**
     * Validiert eine Request-ID
     */
    public boolean isValidRequestId(String requestId) {
        return requestId != null &&
               !requestId.trim().isEmpty() &&
               requestId.startsWith(REQUEST_ID_PREFIX);
    }

    /**
     * Extrahiert Timestamp aus Request-ID falls vorhanden
     */
    public Instant extractTimestamp(String requestId) {
        // Für zukünftige Implementierung - könnte Timestamp in Request-ID einbetten
        return Instant.now();
    }
}
