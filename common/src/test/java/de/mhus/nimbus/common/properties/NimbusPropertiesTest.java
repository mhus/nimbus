package de.mhus.nimbus.common.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für NimbusProperties
 * Folgt Spring Boot Testing Conventions
 */
@SpringBootTest(classes = {NimbusProperties.class})
@EnableConfigurationProperties(NimbusProperties.class)
@TestPropertySource(properties = {
    "nimbus.service.name=test-service",
    "nimbus.service.version=2.0.0",
    "nimbus.service.environment=test",
    "nimbus.service.timeout-ms=60000",
    "nimbus.security.enabled=false",
    "nimbus.security.jwt-issuer=test-issuer",
    "nimbus.security.jwt-expiration-ms=7200000",
    "nimbus.monitoring.metrics-enabled=false",
    "nimbus.monitoring.health-check-enabled=true",
    "nimbus.monitoring.health-check-path=/test/health"
})
class NimbusPropertiesTest {

    @Autowired
    private NimbusProperties nimbusProperties;

    @Test
    void testServiceProperties() {
        var service = nimbusProperties.getService();

        assertEquals("test-service", service.getName());
        assertEquals("2.0.0", service.getVersion());
        assertEquals("test", service.getEnvironment());
        assertEquals(60000, service.getTimeoutMs());
    }

    @Test
    void testSecurityProperties() {
        var security = nimbusProperties.getSecurity();

        assertFalse(security.isEnabled());
        assertEquals("test-issuer", security.getJwtIssuer());
        assertEquals(7200000, security.getJwtExpirationMs());
    }

    @Test
    void testMonitoringProperties() {
        var monitoring = nimbusProperties.getMonitoring();

        assertFalse(monitoring.isMetricsEnabled());
        assertTrue(monitoring.isHealthCheckEnabled());
        assertEquals("/test/health", monitoring.getHealthCheckPath());
    }

    @Test
    void testDefaultValues() {
        // Test mit Standard-Properties
        NimbusProperties defaultProps = new NimbusProperties();

        assertEquals("nimbus-service", defaultProps.getService().getName());
        assertEquals("1.0.0", defaultProps.getService().getVersion());
        assertEquals("development", defaultProps.getService().getEnvironment());
        assertEquals(30000, defaultProps.getService().getTimeoutMs());

        assertTrue(defaultProps.getSecurity().isEnabled());
        assertEquals("nimbus-platform", defaultProps.getSecurity().getJwtIssuer());
        assertEquals(3600000, defaultProps.getSecurity().getJwtExpirationMs());

        assertTrue(defaultProps.getMonitoring().isMetricsEnabled());
        assertTrue(defaultProps.getMonitoring().isHealthCheckEnabled());
        assertEquals("/actuator/health", defaultProps.getMonitoring().getHealthCheckPath());
    }
}
