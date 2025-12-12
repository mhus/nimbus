package de.mhus.nimbus.world.shared.region;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "region.character")
public class RegionCharacterLimitProperties {
    /** Fallback max characters per region if user-specific limit is absent */
    private int maxPerRegion = 10;

    public int getMaxPerRegion() { return maxPerRegion; }
    public void setMaxPerRegion(int maxPerRegion) { this.maxPerRegion = maxPerRegion; }
}

