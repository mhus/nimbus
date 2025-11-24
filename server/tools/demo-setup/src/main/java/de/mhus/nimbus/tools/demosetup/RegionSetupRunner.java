package de.mhus.nimbus.tools.demosetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegionSetupRunner {

    private static final Logger LOG = LoggerFactory.getLogger(RegionSetupRunner.class);

    private final UniverseClientService universeClientService;
    private final RegionClientService regionClientService;
    private final String oasisApiUrl;
    private final String oasisPublicKey; // optional verfügbar

    public RegionSetupRunner(UniverseClientService universeClientService,
                             RegionClientService regionClientService,
                             @Value("${region.oasis.api-url:http://localhost:9050}") String oasisApiUrl,
                             @Value("${region.oasis.public-key:}") String oasisPublicKey) {
        this.universeClientService = universeClientService;
        this.regionClientService = regionClientService;
        this.oasisApiUrl = oasisApiUrl;
        this.oasisPublicKey = oasisPublicKey;
    }

    public void run() {
        String name = "oasis";
        String maintainers = "admin";
        // Lokale Region im Region Server sicherstellen
        if (regionClientService.isConfigured()) {
            regionClientService.ensureRegion(name, oasisApiUrl, maintainers);
        } else {
            LOG.warn("RegionClientService nicht konfiguriert - lokale Region Anlage übersprungen");
        }

        // TODO
    }
}
