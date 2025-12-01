package de.mhus.nimbus.world.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.mhus.nimbus.types.TsEnum;
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
    protected static ObjectMapper objectMapper;
    protected static CloseableHttpClient httpClient;

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

        // Initialize utilities with modern configuration
        objectMapper = new ObjectMapper();

        // Configure ObjectMapper for case-insensitive enum mapping
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        System.out.println("✅ Case-insensitive enum parsing enabled");

        // Add custom enum serializer to serialize enums as lowercase
        SimpleModule enumModule = new SimpleModule();
        enumModule.addSerializer(new JsonSerializer<Enum<?>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Class<Enum<?>> handledType() {
                return (Class<Enum<?>>) (Class<?>) Enum.class;
            }

            @Override
            public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value instanceof TsEnum) {
                    gen.writeString(((TsEnum) value).tsString());
                } else {
                    gen.writeString(value.name().toLowerCase());
                }
            }
        });
        enumModule.addDeserializer(Enum.class, new com.fasterxml.jackson.databind.JsonDeserializer<Enum<?>>() {
            @Override
            public Enum<?> deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                Class<?> enumClass = ctxt.getContextualType().getRawClass();

                if (enumClass != null && enumClass.isEnum()) {
                    Object[] constants = enumClass.getEnumConstants();
                    for (Object constant : constants) {
                        if (constant instanceof TsEnum) {
                            if (((TsEnum) constant).tsString().equalsIgnoreCase(text)) {
                                return (Enum<?>) constant;
                            }
                        } else if (constant instanceof Enum) {
                            if (((Enum<?>) constant).name().equalsIgnoreCase(text)) {
                                return (Enum<?>) constant;
                            }
                        }
                    }
                }

                // Fallback: try standard enum deserialization
                return null;
            }
        });
        objectMapper.registerModule(enumModule);

        // Additional useful configurations for WebSocket message parsing
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

        System.out.println("✅ ObjectMapper configured with:");
        System.out.println("   - Case-insensitive enum parsing support");
        System.out.println("   - Custom lowercase enum serialization (LOGIN -> 'login')");

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
