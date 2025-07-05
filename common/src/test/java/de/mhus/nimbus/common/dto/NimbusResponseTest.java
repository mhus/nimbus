package de.mhus.nimbus.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für NimbusResponse DTO
 * Folgt Spring Boot Testing Conventions
 */
class NimbusResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSuccessResponse() {
        // Given
        String requestId = "test-123";
        String data = "test-data";
        String message = "Success";
        String serviceName = "test-service";

        // When
        NimbusResponse<String> response = NimbusResponse.success(requestId, data, message, serviceName);

        // Then
        assertEquals(requestId, response.getRequestId());
        assertTrue(response.getSuccess());
        assertEquals(data, response.getData());
        assertEquals(message, response.getMessage());
        assertEquals(serviceName, response.getServiceName());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testErrorResponse() {
        // Given
        String requestId = "error-123";
        String message = "Error occurred";
        String errorCode = "ERR_001";
        String serviceName = "test-service";

        // When
        NimbusResponse<Void> response = NimbusResponse.error(requestId, message, errorCode, serviceName);

        // Then
        assertEquals(requestId, response.getRequestId());
        assertFalse(response.getSuccess());
        assertNull(response.getData());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(serviceName, response.getServiceName());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Given
        NimbusResponse<String> response = NimbusResponse.success(
                "test-123", "data", "Success", "test-service");

        // When
        String json = objectMapper.writeValueAsString(response);
        NimbusResponse<?> deserialized = objectMapper.readValue(json, NimbusResponse.class);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"requestId\":\"test-123\""));
        assertEquals(response.getRequestId(), deserialized.getRequestId());
        assertEquals(response.getSuccess(), deserialized.getSuccess());
    }

    @Test
    void testDefaultConstructor() {
        // When
        NimbusResponse<String> response = new NimbusResponse<>();

        // Then
        assertNotNull(response.getTimestamp());
        assertNull(response.getRequestId());
        assertNull(response.getSuccess());
        assertNull(response.getData());
    }
}
