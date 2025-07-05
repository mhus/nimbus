package de.mhus.nimbus.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Standard Response DTO für alle Nimbus Services
 * Folgt Spring Boot Naming Conventions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NimbusResponse<T> {

    @NotBlank
    private String requestId;

    @NotNull
    private Boolean success;

    private T data;

    private String message;

    private String errorCode;

    @NotNull
    private Instant timestamp;

    private String serviceName;

    // Default Constructor für Spring Boot
    public NimbusResponse() {
        this.timestamp = Instant.now();
    }

    // Constructor für Success Response
    public NimbusResponse(String requestId, T data, String message, String serviceName) {
        this();
        this.requestId = requestId;
        this.success = true;
        this.data = data;
        this.message = message;
        this.serviceName = serviceName;
    }

    // Constructor für Error Response
    public NimbusResponse(String requestId, String message, String errorCode, String serviceName) {
        this();
        this.requestId = requestId;
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.serviceName = serviceName;
    }

    // Static Factory Methods (Spring Boot Convention)
    public static <T> NimbusResponse<T> success(String requestId, T data, String message, String serviceName) {
        return new NimbusResponse<>(requestId, data, message, serviceName);
    }

    public static <T> NimbusResponse<T> error(String requestId, String message, String errorCode, String serviceName) {
        return new NimbusResponse<>(requestId, message, errorCode, serviceName);
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
