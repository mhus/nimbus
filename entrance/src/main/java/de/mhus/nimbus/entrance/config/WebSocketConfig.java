package de.mhus.nimbus.entrance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import de.mhus.nimbus.entrance.handler.NimbusWebSocketHandler;

/**
 * WebSocket Konfiguration für das Entrance Modul
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NimbusWebSocketHandler nimbusWebSocketHandler;

    public WebSocketConfig(NimbusWebSocketHandler nimbusWebSocketHandler) {
        this.nimbusWebSocketHandler = nimbusWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(nimbusWebSocketHandler, "/nimbus")
                .setAllowedOrigins("*"); // In Produktion sollten spezifische Origins konfiguriert werden
    }
}
