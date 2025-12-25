package de.mhus.nimbus.world.shared.client;

import de.mhus.nimbus.shared.service.SSettingsService;
import de.mhus.nimbus.shared.settings.SettingInteger;
import de.mhus.nimbus.shared.settings.SettingString;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for inter-server command communication.
 * Loaded from SSettingsService at startup.
 */
@Component
@RequiredArgsConstructor
public class WorldClientSettings {

    private final SSettingsService settingsService;

    private SettingString playerBaseUrl;
    private SettingString lifeBaseUrl;
    private SettingString controlBaseUrl;
    private SettingInteger commandTimeoutMs;

    @PostConstruct
    private void init() {
        playerBaseUrl = settingsService.getString(
                "client.playerBaseUrl",
                "http://localhost:9042"
        );
        lifeBaseUrl = settingsService.getString(
                "client.lifeBaseUrl",
                "http://localhost:9044"
        );
        controlBaseUrl = settingsService.getString(
                "client.controlBaseUrl",
                "http://localhost:9043"
        );
        commandTimeoutMs = settingsService.getInteger(
                "client.commandTimeoutMs",
                5000
        );
    }

    /**
     * Base URL for world-player server.
     * Example: http://world-player:9042
     * Default: http://localhost:9042
     */
    public String getPlayerBaseUrl() {
        return playerBaseUrl.get();
    }

    /**
     * Base URL for world-life server.
     * Example: http://world-life:9044
     * Default: http://localhost:9044
     */
    public String getLifeBaseUrl() {
        return lifeBaseUrl.get();
    }

    /**
     * Base URL for world-control server.
     * Example: http://world-control:9043
     * Default: http://localhost:9043
     */
    public String getControlBaseUrl() {
        return controlBaseUrl.get();
    }

    /**
     * Command timeout in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    public long getCommandTimeoutMs() {
        return commandTimeoutMs.get();
    }
}
