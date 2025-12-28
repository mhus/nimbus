package de.mhus.nimbus.world.shared.access;

import de.mhus.nimbus.shared.service.SSettingsService;
import de.mhus.nimbus.shared.settings.SettingBoolean;
import de.mhus.nimbus.shared.settings.SettingInteger;
import de.mhus.nimbus.shared.settings.SettingString;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for AccessService.
 * Loaded from SSettingsService at startup.
 */
@Component
@RequiredArgsConstructor
public class AccessSettings {

    private final SSettingsService settingsService;

    private SettingInteger tokenExpirationSeconds;
    private SettingString accessUrls;
    private SettingString jumpUrlAgent;
    private SettingString jumpUrlEditor;
    private SettingString jumpUrlViewer;
    private SettingInteger sessionTokenTtlSeconds;
    private SettingInteger agentTokenTtlSeconds;
    private SettingBoolean secureCookies;
    private SettingString cookieDomain;
    private SettingString loginUrl;
    private SettingString logoutUrl;

    @Value( "${nimbus.access.accessUrls:}")
    private String accessUrlsProperty;
    @Value( "${nimbus.access.jumpUrlAgent:}")
    private String jumpUrlAgentProperty;
    @Value( "${nimbus.access.jumpUrlEditor:}")
    private String jumpUrlEditorProperty;
    @Value( "${nimbus.access.jumpUrlViewer:}")
    private String jumpUrlViewerProperty;
    @Value( "${nimbus.access.loginUrl:}")
    private String loginUrlProperty;
    @Value( "${nimbus.access.logoutUrl:}")
    private String logoutUrlProperty;

    @PostConstruct
    private void init() {
        tokenExpirationSeconds = settingsService.getInteger(
                "access.tokenExpirationSeconds",
                300
        );
        accessUrls = settingsService.getString(
                "access.accessUrls",
                "http://localhost:9042/player/aaa/authorize,http://localhost:9043/control/aaa/authorize"
        );
        jumpUrlAgent = settingsService.getString(
                "access.jumpUrlAgent",
                "http://localhost:3002?worldId={worldId}"
        );
        jumpUrlEditor = settingsService.getString(
                "access.jumpUrlEditor",
                "http://localhost:3001?worldId={worldId}&session={session}"
        );
        jumpUrlViewer = settingsService.getString(
                "access.jumpUrlViewer",
                "http://localhost:3000?worldId={worldId}&session={session}"
        );
        sessionTokenTtlSeconds = settingsService.getInteger(
                "access.sessionTokenTtlSeconds",
                86400
        );
        agentTokenTtlSeconds = settingsService.getInteger(
                "access.agentTokenTtlSeconds",
                3600
        );
        secureCookies = settingsService.getBoolean(
                "access.secureCookies",
                false
        );
        cookieDomain = settingsService.getString(
                "access.cookieDomain",
                null
        );
        loginUrl = settingsService.getString(
                "access.loginUrl",
                "http://localhost:3002/dev-login.html"
        );
        logoutUrl = settingsService.getString(
                "access.logoutUrl",
                "http://localhost:3002/dev-login.html"
        );
    }

    /**
     * Access token expiration in seconds.
     * Default: 300 seconds (5 minutes)
     */
    public long getTokenExpirationSeconds() {
        return tokenExpirationSeconds.get();
    }

    /**
     * Cookie URLs for multi-domain cookie setup.
     */
    public List<String> getAccessUrls() {
        if (Strings.isNotBlank(accessUrlsProperty))
            return Arrays.asList(accessUrlsProperty.split(","));
        String urls = accessUrls.get();
        if (urls == null || urls.isBlank()) {
            return List.of();
        }
        return Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Jump URL to redirect after login (agent mode).
     * {worldId} placeholder will be replaced with actual worldId.
     */
    public String getJumpUrlAgent() {
        if (Strings.isNotBlank(jumpUrlAgentProperty)) return jumpUrlAgentProperty;
        return jumpUrlAgent.get();
    }

    /**
     * Jump URL to redirect after login (session mode).
     * {worldId} and {session} placeholders will be replaced.
     */
    public String getJumpUrlEditor() {
        if (Strings.isNotBlank(jumpUrlEditorProperty)) return jumpUrlEditorProperty;
        return jumpUrlEditor.get();
    }

    /**
     * Jump URL to redirect after login (session mode).
     * {worldId} and {session} placeholders will be replaced.
     */
    public String getJumpUrlViewer() {
        if (Strings.isNotBlank(jumpUrlViewerProperty)) return jumpUrlViewerProperty;
        return jumpUrlViewer.get();
    }

    /**
     * Session token TTL in seconds (for agent=false).
     * Default: 86400 seconds (24 hours)
     */
    public long getSessionTokenTtlSeconds() {
        return sessionTokenTtlSeconds.get();
    }

    /**
     * Agent token TTL in seconds (for agent=true).
     * Default: 3600 seconds (1 hour)
     */
    public long getAgentTokenTtlSeconds() {
        return agentTokenTtlSeconds.get();
    }

    /**
     * Whether to use secure cookies (HTTPS only).
     * Should be true in production, false for local development.
     * Default: false
     */
    public boolean isSecureCookies() {
        return secureCookies.get();
    }

    /**
     * Cookie domain for multi-domain setup.
     * If null/empty, cookies are set for the current domain only.
     * Example: ".example.com" for *.example.com
     */
    public String getCookieDomain() {
        return cookieDomain.get();
    }

    public String getLoginUrl() {
        if (Strings.isNotBlank(loginUrlProperty)) return loginUrlProperty;
        return loginUrl.get();
    }

    public String getLogoutUrl() {
        if (Strings.isNotBlank(logoutUrlProperty)) return logoutUrlProperty;
        return logoutUrl.get();
    }
}
