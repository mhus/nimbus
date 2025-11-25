package de.mhus.nimbus.tools.demosetup;

import de.mhus.nimbus.shared.security.SharedClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
            regionClientService.setToken(universeToken); // Admin-Token setzen
            regionClientService.ensureRegion(name, oasisApiUrl, maintainers);
        } else {
            log.warn("RegionClientService nicht konfiguriert - lokale Region Anlage übersprungen");
        }

        // pruefe auf Public Key im Universe Server
        if (!sharedClientService.existsKey(universeClientService.getBaseUrl(), universeToken, "REGION", "PUBLIC", name, "region-jwt-token")) {
            throw new RuntimeException("RegionServer Public Key nicht im Universe vorhanden");
        }

        // Charaktere in oasis anlegen
        if (regionClientService.isConfigured()) {
            log.info("Lege Demo-Charaktere in Region '{}' an...", name);

            // Charaktere in oasis (username:charactername):
            regionClientService.ensureCharacter("samanthaevelyncook", name, "art3mis", "Art3mis");
            regionClientService.ensureCharacter("wadewatts", name, "parzival", "Parzival");
            regionClientService.ensureCharacter("helenharris", name, "aech", "Aech");
            regionClientService.ensureCharacter("toshiroyoshiaki", name, "daito", "Daito");
            regionClientService.ensureCharacter("akihidekaratsu", name, "shoto", "Shoto");

            log.info("Demo-Charaktere erfolgreich angelegt/überprüft");
        } else {
            log.warn("RegionClientService nicht konfiguriert - Character-Anlage übersprungen");
        }
    }
}
