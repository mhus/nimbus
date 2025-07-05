package de.mhus.nimbus.common.exception;

import de.mhus.nimbus.common.dto.NimbusResponse;
import de.mhus.nimbus.common.util.RequestIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Globaler Exception Handler für alle Nimbus Services
 * Folgt Spring Boot Naming Conventions
 */
@RestControllerAdvice
public class NimbusGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NimbusGlobalExceptionHandler.class);

    private final RequestIdUtils requestIdUtils;

    public NimbusGlobalExceptionHandler(RequestIdUtils requestIdUtils) {
        this.requestIdUtils = requestIdUtils;
    }

    /**
     * Handler für Nimbus-spezifische Exceptions
     */
    @ExceptionHandler(NimbusException.class)
    public ResponseEntity<NimbusResponse<Void>> handleNimbusException(NimbusException ex, WebRequest request) {
        String requestId = extractRequestId(request);

        logger.error("Nimbus exception occurred: requestId={}, errorCode={}, service={}",
                    requestId, ex.getErrorCode(), ex.getServiceName(), ex);

        NimbusResponse<Void> response = NimbusResponse.error(
                requestId, ex.getMessage(), ex.getErrorCode(), ex.getServiceName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler für Validation Exceptions
     */
    @ExceptionHandler(NimbusValidationException.class)
    public ResponseEntity<NimbusResponse<Void>> handleValidationException(NimbusValidationException ex, WebRequest request) {
        String requestId = extractRequestId(request);

        logger.warn("Validation exception occurred: requestId={}, service={}",
                   requestId, ex.getServiceName(), ex);

        NimbusResponse<Void> response = NimbusResponse.error(
                requestId, ex.getMessage(), ex.getErrorCode(), ex.getServiceName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler für Spring Validation Exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NimbusResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        String requestId = extractRequestId(request);

        StringBuilder message = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                message.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; "));

        logger.warn("Method argument validation failed: requestId={}", requestId, ex);

        NimbusResponse<Void> response = NimbusResponse.error(
                requestId, message.toString(), "VALIDATION_ERROR", "nimbus-common");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler für alle anderen Exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<NimbusResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        String requestId = extractRequestId(request);

        logger.error("Unexpected exception occurred: requestId={}", requestId, ex);

        NimbusResponse<Void> response = NimbusResponse.error(
                requestId, "Internal server error", "INTERNAL_ERROR", "nimbus-common");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extrahiert Request-ID aus WebRequest oder generiert eine neue
     */
    private String extractRequestId(WebRequest request) {
        String requestId = request.getHeader("X-Nimbus-Request-ID");
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = requestIdUtils.generateRequestId("error");
        }
        return requestId;
    }
}
