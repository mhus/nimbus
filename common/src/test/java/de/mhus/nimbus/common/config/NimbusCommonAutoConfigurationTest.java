package de.mhus.nimbus.common.config;

import de.mhus.nimbus.common.properties.NimbusProperties;
import de.mhus.nimbus.common.util.RequestIdUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test für Nimbus Common Auto-Configuration
 * Folgt Spring Boot Testing Conventions
 */
@SpringBootTest(classes = {NimbusCommonAutoConfiguration.class, NimbusCommonConfiguration.class})
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "nimbus.service.name=integration-test"
})
class NimbusCommonAutoConfigurationTest {

    @Autowired
    private NimbusProperties nimbusProperties;

    @Autowired
    private RequestIdUtils requestIdUtils;

    @Autowired
    private RestTemplate nimbusRestTemplate;

    @Autowired
    private Clock nimbusClock;

    @Test
    void testAutoConfigurationLoadsAllBeans() {
        // Verify all expected beans are loaded
        assertNotNull(nimbusProperties);
        assertNotNull(requestIdUtils);
        assertNotNull(nimbusRestTemplate);
        assertNotNull(nimbusClock);
    }

    @Test
    void testPropertiesAreConfigured() {
        assertEquals("integration-test", nimbusProperties.getService().getName());
    }

    @Test
    void testBeansAreFunctional() {
        // Test RequestIdUtils
        String requestId = requestIdUtils.generateRequestId();
        assertTrue(requestIdUtils.isValidRequestId(requestId));

        // Test Clock
        assertNotNull(nimbusClock.instant());

        // Test RestTemplate
        assertNotNull(nimbusRestTemplate.getRequestFactory());
    }
}
