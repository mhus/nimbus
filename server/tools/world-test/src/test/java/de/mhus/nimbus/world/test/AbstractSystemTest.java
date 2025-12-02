package de.mhus.nimbus.world.test;

import de.mhus.nimbus.shared.engine.EngineMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Basis-Klasse für alle externen System-Tests.
 * Lädt Konfiguration und stellt gemeinsame Utilities bereit.
 */
public abstract class AbstractSystemTest {

    protected static Properties properties;
    protected static CloseableHttpClient httpClient;
    protected static EngineMapper objectMapper = new EngineMapper();

    // Test Configuration
    protected static String webSocketUrl;
    protected static String playerUrl;
    protected static String editorUrl;
    protected static String loginUsername;
    protected static String loginPassword;
    protected static String worldId;
    protected static String clientType;

    @BeforeAll
    @SuppressWarnings("deprecation") // No modern alternative available for ACCEPT_CASE_INSENSITIVE_ENUMS
    static void setUpTestConfiguration() throws IOException {
        // Load properties
        properties = new Properties();
        try (InputStream input = AbstractSystemTest.class
                .getClassLoader()
                .getResourceAsStream("application.yaml")) {
            if (input != null) {
                // Simplified YAML parsing - only key-value pairs
                String content = new String(input.readAllBytes());
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.contains(":") && !line.trim().startsWith("#")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            properties.setProperty(key, value);
                        }
                    }
                }
            }
        }

        // Initialize configuration
        webSocketUrl = getProperty("test.server.websocket.url", "ws://localhost:3011");
        playerUrl = getProperty("test.server.player.url", "http://localhost:3011");
        editorUrl = getProperty("test.server.editor.url", "http://localhost:3011");
        loginUsername = getProperty("test.login.username", "testuser");
        loginPassword = getProperty("test.login.password", "testpass");
        worldId = getProperty("test.login.worldId", "test-world");
        clientType = getProperty("test.login.clientType", "web");

        System.out.println("✅ ObjectMapper configured");

        httpClient = HttpClients.createDefault();
    }

    protected static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    protected static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Return default on parse error
            }
        }
        return defaultValue;
    }
}
