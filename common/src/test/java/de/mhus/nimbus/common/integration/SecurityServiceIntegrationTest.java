package de.mhus.nimbus.common.integration;

import de.mhus.nimbus.common.config.NimbusCommonAutoConfiguration;
import de.mhus.nimbus.common.service.SecurityService;
import de.mhus.nimbus.common.util.RequestIdUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test für SecurityService Bean
 * Zeigt die Verwendung in einem Spring Boot Kontext
 */
@SpringBootTest(classes = {NimbusCommonAutoConfiguration.class})
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "nimbus.security.enabled=true",
    "nimbus.security.kafka.enabled=false", // Kafka deaktiviert für Test
    "nimbus.service.name=security-test"
})
class SecurityServiceIntegrationTest {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private RequestIdUtils requestIdUtils;

    @Test
    void testSecurityServiceBeanIsLoaded() {
        // Verify that SecurityService bean is properly loaded
        assertNotNull(securityService);
        assertNotNull(requestIdUtils);
    }

    @Test
    void testRequestIdGeneration() {
        // Test that dependencies work correctly
        String requestId = requestIdUtils.generateRequestId("test");
        assertTrue(requestId.startsWith("nimbus-test-"));
    }

    // Note: Vollständige Login-Tests würden einen laufenden Kafka-Cluster erfordern
    // Diese Tests zeigen nur die Bean-Konfiguration und Dependency Injection
}
