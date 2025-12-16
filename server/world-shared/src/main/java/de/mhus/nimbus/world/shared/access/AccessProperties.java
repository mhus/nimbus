package de.mhus.nimbus.world.shared.access;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for AccessService.
 * Loaded from application.yml with prefix 'world.access'.
 */
@Component
@ConfigurationProperties(prefix = "world.access")
@Data
public class AccessProperties {

    /**
     * Access token expiration in seconds.
     * Default: 300 seconds (5 minutes)
     */
    private long tokenExpirationSeconds = 300;

    /**
     * Cookie URLs for multi-domain cookie setup.
     * Hardcoded for now, will be made dynamic later.
     */
    private List<String> accessUrls = List.of(
            "http://localhost:9042/player/aaa/authorize",
            "http://localhost:9043/control/aaa/authorize"
    );

    /**
     * Jump URL to redirect after login.
     * Hardcoded for now, will be made dynamic later.
     * {worldId} placeholder will be replaced with actual worldId.
     */
    private String jumpUrlAgent = "http://localhost:3002?worldId={worldId}";
    private String jumpUrlSession = "http://localhost:3001?worldId={worldId}&session={session}";

    /**
     * Session token TTL in seconds (for agent=false).
     * Default: 86400 seconds (24 hours)
     */
    private long sessionTokenTtlSeconds = 86400L;

    /**
     * Agent token TTL in seconds (for agent=true).
     * Default: 3600 seconds (1 hour)
     */
    private long agentTokenTtlSeconds = 3600L;

    /**
     * Whether to use secure cookies (HTTPS only).
     * Should be true in production, false for local development.
     * Default: false
     */
    private boolean secureCookies = false;

    /**
     * Cookie domain for multi-domain setup.
     * If null/empty, cookies are set for the current domain only.
     * Example: ".example.com" for *.example.com
     */
    private String cookieDomain = null;

    private String loginUrl = "http://localhost:3002/dev-login.html";

    private String logoutUrl = "http://localhost:3002/dev-login.html";


}
