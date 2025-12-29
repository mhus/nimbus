package de.mhus.nimbus.world.player.config;

import de.mhus.nimbus.shared.service.SSettingsService;
import de.mhus.nimbus.shared.settings.SettingString;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerSettings {

    private final SSettingsService settingsService;

    private SettingString websocketUrl;
    @Value("${nimbus.server.websocketUrl:}")
    private String websocketUrlOverwrite;

    @PostConstruct
    private void init() {
        websocketUrl = settingsService.getString(
                "server.websocketUrl",
                "ws://localhost:9042/player/ws",
                websocketUrlOverwrite
        );
    }

    /**
     * WebSocket URL for client connection.
     * Default: ws://localhost:9042/ws
     */
    public String getWebsocketUrl() {
        return websocketUrl.get();
    }
}
