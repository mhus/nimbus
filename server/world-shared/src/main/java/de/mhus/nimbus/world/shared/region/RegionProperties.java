package de.mhus.nimbus.world.shared.region;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Konfiguration für den Zugriff vom Region-Modul auf den Universe-Server.
 */
@Component
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegionProperties {

    @Value("${universe.server.url}")
    private String universeBaseUrl;

    @Value("${region.server.url}")
    private String regionServerUrl;

    @Value("${region.server.id}")
    private String regionServerId;

}
