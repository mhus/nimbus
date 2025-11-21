package de.mhus.nimbus.region.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT-bezogene Properties für das Region-Modul.
 */
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class RegionJwtProperties {

    /**
     * UUID-Teil des Welt-KeyIds. Owner ist die worldId.
     */
    private String worldKeyUuid;

    public String getWorldKeyUuid() {
        return worldKeyUuid;
    }

    public void setWorldKeyUuid(String worldKeyUuid) {
        this.worldKeyUuid = worldKeyUuid;
    }
}
