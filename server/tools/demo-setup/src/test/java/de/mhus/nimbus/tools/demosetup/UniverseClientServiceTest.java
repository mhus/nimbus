package de.mhus.nimbus.tools.demosetup;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class UniverseClientServiceTest {

    @Test
    void notConfiguredIfEmpty() {
        UniverseClientService svc = new UniverseClientService("");
        assertFalse(svc.isConfigured());
    }

    @Test
    void configuredIfUrl() {
        UniverseClientService svc = new UniverseClientService("http://localhost:9040");
        assertTrue(svc.isConfigured());
    }
}

