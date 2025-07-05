package de.mhus.nimbus.common.exception;

/**
 * Basis-Exception für alle Nimbus-spezifischen Exceptions
 * Folgt Spring Boot Naming Conventions
 */
public class NimbusException extends RuntimeException {

    private final String errorCode;
    private final String serviceName;

    public NimbusException(String message) {
        super(message);
        this.errorCode = "NIMBUS_ERROR";
        this.serviceName = "unknown";
    }

    public NimbusException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.serviceName = "unknown";
    }

    public NimbusException(String message, String errorCode, String serviceName) {
        super(message);
        this.errorCode = errorCode;
        this.serviceName = serviceName;
    }

    public NimbusException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NIMBUS_ERROR";
        this.serviceName = "unknown";
    }

    public NimbusException(String message, Throwable cause, String errorCode, String serviceName) {
        super(message, cause);
        this.errorCode = errorCode;
        this.serviceName = serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getServiceName() {
        return serviceName;
    }
}
