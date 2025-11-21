package de.mhus.nimbus.region.universe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguration für den Zugriff vom Region-Modul auf den Universe-Server.
 */
@Component
@ConfigurationProperties(prefix = "universe.client")
public class RUniverseProperties {

    /**
     * Basis-URL des Universe-Servers, z. B. "http://localhost:8080".
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * UUID-Teil des Region-KeyIds zum Signieren der Region-Token.
     */
    private String regionKeyUuid;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getRegionKeyUuid() { return regionKeyUuid; }
    public void setRegionKeyUuid(String regionKeyUuid) { this.regionKeyUuid = regionKeyUuid; }
}
