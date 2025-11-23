package de.mhus.nimbus.world.shared.region;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguration für den Zugriff vom World-Modul auf den Region-Server.
 */
@Component
@ConfigurationProperties(prefix = "region.client")
public class WRegionProperties {

    /**
     * Basis-URL des Region-Servers, z. B. "http://localhost:8081".
     */
    private String baseUrl = "http://localhost:8081";

    /**
     * UUID-Teil des WORLD-KeyIds zum Signieren der World-Token.
     * Der Owner ist die worldId.
     */
    private String worldKeyUuid;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getWorldKeyUuid() { return worldKeyUuid; }
    public void setWorldKeyUuid(String worldKeyUuid) { this.worldKeyUuid = worldKeyUuid; }
}
