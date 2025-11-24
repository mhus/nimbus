package de.mhus.nimbus.region;

import de.mhus.nimbus.shared.security.SharedClientService;
import de.mhus.nimbus.shared.security.KeyIntent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Registriert beim Start den Public Key des Region Servers (regionServerPublicKey.txt)
 * beim Universe Server über dessen /shared/key Endpoint, falls noch nicht vorhanden.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServerRegistrationService {

    private final SharedClientService sharedClientService;

    @Value("${universe.base.url:http://localhost:8080}")
    private String universeBaseUrl;

    @Value("${region.server.id:local-region-server}")
    private String regionServerId;

    @PostConstruct
    public void register() {
        var keyOpt = sharedClientService.readPublicKeyFromFile(Path.of("confidential/regionServerPublicKey.txt"));
        if (keyOpt.isEmpty()) {
            log.warn("Kein regionServerPublicKey.txt gefunden – Key wird nicht registriert.");
            return;
        }
        boolean exists = sharedClientService.existsKey(universeBaseUrl, "REGION", "PUBLIC", regionServerId, KeyIntent.MAIN_JWT_TOKEN);
        if (exists) {
            log.info("Public Key bereits vorhanden für owner={} intent={}", regionServerId, KeyIntent.MAIN_JWT_TOKEN);
            return;
        }
        var base64Opt = sharedClientService.toBase64PublicKey(keyOpt.get());
        if (base64Opt.isEmpty()) {
            log.error("Konnte Public Key nicht in Base64 konvertieren.");
            return;
        }
        var created = sharedClientService.createKey(universeBaseUrl, "REGION", "PUBLIC", keyOpt.get().getAlgorithm(), regionServerId, KeyIntent.MAIN_JWT_TOKEN, regionServerId+":"+KeyIntent.MAIN_JWT_TOKEN+":"+java.util.UUID.randomUUID(), base64Opt.get());
        if (created.isEmpty()) {
            log.error("Registrierung des Region Server Public Keys fehlgeschlagen (owner={} intent={})", regionServerId, KeyIntent.MAIN_JWT_TOKEN);
        } else {
            log.info("Region Server Public Key registriert (id={})", created.get().getId());
        }
    }
}
