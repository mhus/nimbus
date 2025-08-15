package de.mhus.nimbus.world.bridge.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebSocketCommandInfoTest {

    @Test
    void testFullConstructor() {
        WebSocketCommandInfo info = new WebSocketCommandInfo("bridge", "ping", "Test command", false, false);

        assertEquals("bridge", info.getService());
        assertEquals("ping", info.getCommand());
        assertEquals("Test command", info.getDescription());
        assertFalse(info.isWorldRequired());
        assertFalse(info.isAuthenticationRequired());
    }

    @Test
    void testBackwardCompatibilityConstructor() {
        WebSocketCommandInfo info = new WebSocketCommandInfo("bridge", "test", "Test command");

        assertEquals("bridge", info.getService());
        assertEquals("test", info.getCommand());
        assertEquals("Test command", info.getDescription());
        assertTrue(info.isWorldRequired()); // Default
        assertTrue(info.isAuthenticationRequired()); // Default
    }

    @Test
    void testWorldRequiredConstructor() {
        WebSocketCommandInfo info = new WebSocketCommandInfo("bridge", "use", "Use world command", false);

        assertEquals("bridge", info.getService());
        assertEquals("use", info.getCommand());
        assertEquals("Use world command", info.getDescription());
        assertFalse(info.isWorldRequired());
        assertTrue(info.isAuthenticationRequired()); // Default
    }

    @Test
    void testSettersAndGetters() {
        WebSocketCommandInfo info = new WebSocketCommandInfo();

        info.setService("bridge");
        info.setCommand("ping");
        info.setDescription("Ping command");
        info.setWorldRequired(false);
        info.setAuthenticationRequired(false);

        assertEquals("bridge", info.getService());
        assertEquals("ping", info.getCommand());
        assertEquals("Ping command", info.getDescription());
        assertFalse(info.isWorldRequired());
        assertFalse(info.isAuthenticationRequired());
    }
}
