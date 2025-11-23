package de.mhus.nimbus.region;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguration für den Zugriff vom Region-Modul auf den Universe-Server.
 */
@Component
@ConfigurationProperties(prefix = "region")
@Data
public class RegionProperties {

    /**
     * Basis-URL des Universe-Servers, z. B. "http://localhost:8080".
     */
    private String universeBaseUrl = "http://localhost:9040";

}
