package de.mhus.nimbus.tools.demosetup;

import de.mhus.nimbus.shared.security.SharedClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class RegionSetupRunner {

    private final SharedClientService sharedClientService = new SharedClientService();
    private final RegionClientService regionClientService;
    private final UniverseClientService universeClientService;
    @Value("${region.oasis.api-url:http://localhost:9050}")
    private String oasisApiUrl;
    @Value("${region.oasis.public-key:}")
    private String oasisPublicKey; // optional verfügbar

    public void run(String universeToken) {
        String name = "oasis";
        String maintainers = "admin";
        // Lokale Region im Region Server sicherstellen
        if (regionClientService.isConfigured()) {
            regionClientService.ensureRegion(name, oasisApiUrl, maintainers);
        } else {
            log.warn("RegionClientService nicht konfiguriert - lokale Region Anlage übersprungen");
        }

        // pruefe auf Public Key im Universe Server
        if (!sharedClientService.existsKey(universeClientService.getBaseUrl(), universeToken, "REGION", "PUBLIC", name, "region-jwt-token")) {
            throw new RuntimeException("RegionServer Public Key nicht im Universe vorhanden");
        }
    }
}
