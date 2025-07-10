package de.mhus.nimbus.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für NimbusResponse DTO
 * Testet Success- und Error-Responses sowie JSON-Serialisierung
 */
class NimbusResponseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

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
        assertTrue(response.isSuccess());
        assertEquals(data, response.getData());
        assertEquals(message, response.getMessage());
        assertEquals(serviceName, response.getServiceName());
        assertNull(response.getErrorCode());
        assertNull(response.getErrorMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testErrorResponse() {
        // Given
        String requestId = "test-123";
        String errorCode = "VALIDATION_ERROR";
        String errorMessage = "Invalid input";
        String serviceName = "test-service";

        // When
        NimbusResponse<String> response = NimbusResponse.error(requestId, errorCode, errorMessage, serviceName);

        // Then
        assertEquals(requestId, response.getRequestId());
        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertNull(response.getData());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorMessage, response.getErrorMessage());
        assertEquals(serviceName, response.getServiceName());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Given
        NimbusResponse<String> response = NimbusResponse.success(
                "test-123", "data", "Success", "test-service");

        // When
        String json = mapper.writeValueAsString(response);
        NimbusResponse<?> deserialized = mapper.readValue(json, NimbusResponse.class);

        // Then
        assertNotNull(json);
        assertEquals(response.getRequestId(), deserialized.getRequestId());
        assertEquals(response.isSuccess(), deserialized.isSuccess());
        assertEquals(response.getData(), deserialized.getData());
        assertEquals(response.getMessage(), deserialized.getMessage());
        assertEquals(response.getServiceName(), deserialized.getServiceName());
    }

    @Test
    void testEmptyResponse() {
        // When
        NimbusResponse<String> response = new NimbusResponse<>();

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertNull(response.getData());
        assertFalse(response.hasData());
        assertFalse(response.hasErrorCode());
    }

    @Test
    void testSuccessWithoutMessage() {
        // Given
        String requestId = "test-123";
        String data = "test-data";

        // When
        NimbusResponse<String> response = NimbusResponse.success(requestId, data);

        // Then
        assertEquals(requestId, response.getRequestId());
        assertTrue(response.isSuccess());
        assertEquals(data, response.getData());
        assertNull(response.getMessage());
        assertNull(response.getServiceName());
        assertTrue(response.hasData());
    }

    @Test
    void testSuccessWithMessage() {
        // Given
        String requestId = "test-123";
        String data = "test-data";
        String message = "Operation completed";

        // When
        NimbusResponse<String> response = NimbusResponse.success(requestId, data, message);

        // Then
        assertEquals(requestId, response.getRequestId());
        assertTrue(response.isSuccess());
        assertEquals(data, response.getData());
        assertEquals(message, response.getMessage());
        assertNull(response.getServiceName());
    }

    @Test
    void testErrorWithoutServiceName() {
        // Given
        String requestId = "test-123";
        String errorCode = "NOT_FOUND";
        String errorMessage = "Resource not found";

        // When
        NimbusResponse<String> response = NimbusResponse.error(requestId, errorCode, errorMessage);

        // Then
        assertEquals(requestId, response.getRequestId());
        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorMessage, response.getErrorMessage());
        assertNull(response.getServiceName());
        assertTrue(response.hasErrorCode());
    }

    @Test
    void testComplexDataType() {
        // Given
        String requestId = "test-123";
        TestData testData = new TestData("name", 42);

        // When
        NimbusResponse<TestData> response = NimbusResponse.success(requestId, testData, "Success", "test-service");

        // Then
        assertEquals(requestId, response.getRequestId());
        assertTrue(response.isSuccess());
        assertEquals(testData, response.getData());
        assertEquals("name", response.getData().getName());
        assertEquals(42, response.getData().getValue());
    }

    @Test
    void testNullData() {
        // Given
        String requestId = "test-123";

        // When
        NimbusResponse<String> response = NimbusResponse.success(requestId, null, "Success", "test-service");

        // Then
        assertEquals(requestId, response.getRequestId());
        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertFalse(response.hasData());
    }

    // Helper class for testing complex data types
    private static class TestData {
        private String name;
        private int value;

        public TestData() {}

        public TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestData testData = (TestData) obj;
            return value == testData.value &&
                   (name != null ? name.equals(testData.name) : testData.name == null);
        }
    }
}
