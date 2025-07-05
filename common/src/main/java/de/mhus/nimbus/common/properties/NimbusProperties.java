package de.mhus.nimbus.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemeinsame Nimbus Properties nach Spring Boot Naming Conventions
 */
@ConfigurationProperties(prefix = "nimbus")
public class NimbusProperties {

    private final Service service = new Service();
    private final Security security = new Security();
    private final Monitoring monitoring = new Monitoring();

    public Service getService() {
        return service;
    }

    public Security getSecurity() {
        return security;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public static class Service {
        private String name = "nimbus-service";
        private String version = "1.0.0";
        private String environment = "development";
        private long timeoutMs = 30000;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Security {
        private boolean enabled = true;
        private String jwtIssuer = "nimbus-platform";
        private long jwtExpirationMs = 3600000; // 1 Stunde
        private final Kafka kafka = new Kafka();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJwtIssuer() {
            return jwtIssuer;
        }

        public void setJwtIssuer(String jwtIssuer) {
            this.jwtIssuer = jwtIssuer;
        }

        public long getJwtExpirationMs() {
            return jwtExpirationMs;
        }

        public void setJwtExpirationMs(long jwtExpirationMs) {
            this.jwtExpirationMs = jwtExpirationMs;
        }

        public Kafka getKafka() {
            return kafka;
        }

        public static class Kafka {
            private boolean enabled = false;
            private long timeoutMs = 30000; // 30 Sekunden
            private String groupIdPrefix = "nimbus-common";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public long getTimeoutMs() {
                return timeoutMs;
            }

            public void setTimeoutMs(long timeoutMs) {
                this.timeoutMs = timeoutMs;
            }

            public String getGroupIdPrefix() {
                return groupIdPrefix;
            }

            public void setGroupIdPrefix(String groupIdPrefix) {
                this.groupIdPrefix = groupIdPrefix;
            }
        }
    }

    public static class Monitoring {
        private boolean metricsEnabled = true;
        private boolean healthCheckEnabled = true;
        private String healthCheckPath = "/actuator/health";

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public boolean isHealthCheckEnabled() {
            return healthCheckEnabled;
        }

        public void setHealthCheckEnabled(boolean healthCheckEnabled) {
            this.healthCheckEnabled = healthCheckEnabled;
        }

        public String getHealthCheckPath() {
            return healthCheckPath;
        }

        public void setHealthCheckPath(String healthCheckPath) {
            this.healthCheckPath = healthCheckPath;
        }
    }
}
