package de.mhus.nimbus.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für RequestIdUtils
 * Folgt Spring Boot Testing Conventions
 */
@SpringBootTest(classes = {RequestIdUtils.class})
@TestPropertySource(properties = "spring.main.banner-mode=off")
class RequestIdUtilsTest {

    @Autowired
    private RequestIdUtils requestIdUtils;

    @Test
    void testGenerateRequestId() {
        // When
        String requestId = requestIdUtils.generateRequestId();

        // Then
        assertNotNull(requestId);
        assertTrue(requestId.startsWith("nimbus-"));
        assertTrue(requestIdUtils.isValidRequestId(requestId));
    }

    @Test
    void testGenerateRequestIdWithServiceName() {
        // Given
        String serviceName = "test-service";

        // When
        String requestId = requestIdUtils.generateRequestId(serviceName);

        // Then
        assertNotNull(requestId);
        assertTrue(requestId.startsWith("nimbus-" + serviceName + "-"));
        assertTrue(requestIdUtils.isValidRequestId(requestId));
    }

    @Test
    void testIsValidRequestId() {
        // Valid request IDs
        assertTrue(requestIdUtils.isValidRequestId("nimbus-123456"));
        assertTrue(requestIdUtils.isValidRequestId("nimbus-service-uuid"));

        // Invalid request IDs
        assertFalse(requestIdUtils.isValidRequestId(null));
        assertFalse(requestIdUtils.isValidRequestId(""));
        assertFalse(requestIdUtils.isValidRequestId("   "));
        assertFalse(requestIdUtils.isValidRequestId("invalid-123"));
        assertFalse(requestIdUtils.isValidRequestId("test-nimbus-123"));
    }

    @Test
    void testExtractTimestamp() {
        // Given
        String requestId = requestIdUtils.generateRequestId();

        // When
        var timestamp = requestIdUtils.extractTimestamp(requestId);

        // Then
        assertNotNull(timestamp);
    }
}
