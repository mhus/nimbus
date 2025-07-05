package de.mhus.nimbus.common.exception;

/**
 * Exception für Service-Validierungsfehler
 * Folgt Spring Boot Naming Conventions
 */
public class NimbusValidationException extends NimbusException {

    public NimbusValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }

    public NimbusValidationException(String message, String serviceName) {
        super(message, "VALIDATION_ERROR", serviceName);
    }

    public NimbusValidationException(String message, String serviceName, Throwable cause) {
        super(message, "VALIDATION_ERROR", serviceName, cause);
    }
}
