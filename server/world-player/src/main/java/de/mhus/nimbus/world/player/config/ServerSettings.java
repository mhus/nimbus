package de.mhus.nimbus.world.player.config;

import de.mhus.nimbus.shared.service.SSettingsService;
import de.mhus.nimbus.shared.settings.SettingString;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerSettings {

    private final SSettingsService settingsService;

    private SettingString websocketUrl;
    private SettingString exitUrl;

    @PostConstruct
    private void init() {
        websocketUrl = settingsService.getString(
                "server.websocketUrl",
                "ws://localhost:9042/ws"
        );
        exitUrl = settingsService.getString(
                "server.exitUrl",
                "http://localhost:3002/dev-login.html"
        );
    }

    /**
     * WebSocket URL for client connection.
     * Default: ws://localhost:9042/ws
     */
    public String getWebsocketUrl() {
        return websocketUrl.get();
    }

    /**
     * Exit URL where clients are redirected after leaving the world.
     * Default: http://localhost:3002/dev-login.html
     */
    public String getExitUrl() {
        return exitUrl.get();
    }
}
