package de.mhus.nimbus.registry.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test für Consumer mit H2-Datenbank
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class LookupConsumerTest {

    @Test
    void contextLoads() {
        // Test ob der Spring Context erfolgreich lädt mit H2-Datenbank
    }
}
