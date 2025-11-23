package de.mhus.nimbus.region.universe;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguration für den Zugriff vom Region-Modul auf den Universe-Server.
 */
@Component
@ConfigurationProperties(prefix = "universe.client")
@Data
public class RUniverseProperties {

    /**
     * Basis-URL des Universe-Servers, z. B. "http://localhost:8080".
     */
    private String baseUrl = "http://localhost:8080";

}
