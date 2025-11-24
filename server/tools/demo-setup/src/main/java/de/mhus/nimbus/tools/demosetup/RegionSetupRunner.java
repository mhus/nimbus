package de.mhus.nimbus.tools.demosetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegionSetupRunner {

    private static final Logger LOG = LoggerFactory.getLogger(RegionSetupRunner.class);

    private final UniverseClientService universeClientService;
    private final String oasisApiUrl;
    private final String oasisPublicKey; // optional verfügbar

    public RegionSetupRunner(UniverseClientService universeClientService,
                             @Value("${region.oasis.api-url:http://localhost:9050}") String oasisApiUrl,
                             @Value("${region.oasis.public-key:}") String oasisPublicKey) {
        this.universeClientService = universeClientService;
        this.oasisApiUrl = oasisApiUrl;
        this.oasisPublicKey = oasisPublicKey;
    }

    public void run() {
        if (!universeClientService.isConfigured()) {
            LOG.warn("Universe Service nicht konfiguriert - überspringe Region Setup");
            return;
        }
        if (!universeClientService.hasToken()) {
            LOG.warn("Kein Token - Region Setup erst nach Admin Login möglich");
            return;
        }
        String name = "oasis";
        universeClientService.ensureRegion(name, oasisApiUrl, oasisPublicKey);
        boolean hasKey = universeClientService.checkRegionPublicKey(name);
        if (!hasKey) {
            LOG.warn("Public Key für Region '{}' fehlt noch", name);
        }
    }
}

