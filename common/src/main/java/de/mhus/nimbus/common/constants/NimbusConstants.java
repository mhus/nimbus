package de.mhus.nimbus.common.constants;

/**
 * Gemeinsame Konstanten für das Nimbus-System
 * Folgt Spring Boot Naming Conventions
 */
public final class NimbusConstants {

    private NimbusConstants() {
        // Utility class - private constructor
    }

    // HTTP Header Konstanten
    public static final class Headers {
        public static final String REQUEST_ID = "X-Nimbus-Request-ID";
        public static final String SERVICE_NAME = "X-Nimbus-Service";
        public static final String VERSION = "X-Nimbus-Version";
        public static final String ENVIRONMENT = "X-Nimbus-Environment";

        private Headers() {}
    }

    // Kafka Topic Konstanten
    public static final class Topics {
        public static final String USER_LOOKUP_REQUEST = "user-lookup-request";
        public static final String USER_LOOKUP_RESPONSE = "user-lookup-response";
        public static final String PLAYER_CHARACTER_LOOKUP_REQUEST = "player-character-lookup-request";
        public static final String PLAYER_CHARACTER_LOOKUP_RESPONSE = "player-character-lookup-response";
        public static final String LOGIN_REQUEST = "login-request";
        public static final String LOGIN_RESPONSE = "login-response";
        public static final String PUBLIC_KEY_REQUEST = "public-key-request";
        public static final String PUBLIC_KEY_RESPONSE = "public-key-response";

        private Topics() {}
    }

    // Environment Konstanten
    public static final class Environments {
        public static final String DEVELOPMENT = "development";
        public static final String TEST = "test";
        public static final String STAGING = "staging";
        public static final String PRODUCTION = "production";

        private Environments() {}
    }

    // Service Namen
    public static final class Services {
        public static final String IDENTITY = "nimbus-identity";
        public static final String REGISTRY = "nimbus-registry";
        public static final String COMMON = "nimbus-common";

        private Services() {}
    }

    // Default Werte
    public static final class Defaults {
        public static final long REQUEST_TIMEOUT_MS = 30000;
        public static final int RETRY_ATTEMPTS = 3;
        public static final long JWT_EXPIRATION_MS = 3600000; // 1 Stunde

        private Defaults() {}
    }
}
