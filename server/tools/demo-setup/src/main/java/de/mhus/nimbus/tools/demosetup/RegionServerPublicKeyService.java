package de.mhus.nimbus.tools.demosetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class RegionServerPublicKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(RegionServerPublicKeyService.class);

    private final String configuredPath;
    private String cachedBase64; // Cache des gelesenen Public Keys (Base64)

    public RegionServerPublicKeyService(@Value("${region.server.public-key-file:}") String configuredPath) {
        this.configuredPath = configuredPath == null ? "" : configuredPath.trim();
    }

    public Optional<String> getPublicKeyBase64() {
        if (cachedBase64 != null) return Optional.of(cachedBase64);
        Optional<String> pk = loadPublicKeyBase64();
        cachedBase64 = pk.orElse(null);
        return pk;
    }

    public Optional<PublicKey> getPublicKey() {
        return getPublicKeyBase64().flatMap(b64 -> {
            try {
                byte[] der = Base64.getDecoder().decode(b64);
                PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
                return Optional.of(pub);
            } catch (Exception e) {
                LOG.error("Kann Public Key aus Base64 nicht parsen: {}", e.toString());
                return Optional.empty();
            }
        });
    }

    private Optional<String> loadPublicKeyBase64() {
        List<Path> candidates = new ArrayList<>();
        String env = System.getenv("REGION_SERVER_PUBLIC_KEY_FILE");
        if (!configuredPath.isBlank()) candidates.add(Path.of(configuredPath));
        if (env != null && !env.isBlank()) candidates.add(Path.of(env.trim()));

        // Standardpfade relativ zum Startpunkt (analog AdminService mehrfach aufsteigend)
        candidates.add(Path.of("confidential/regionServerPublicKey.txt"));
        candidates.add(Path.of("../confidential/regionServerPublicKey.txt"));
        candidates.add(Path.of("../../confidential/regionServerPublicKey.txt"));
        candidates.add(Path.of("../../../confidential/regionServerPublicKey.txt"));

        for (Path p : candidates) {
            try {
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    String raw = Files.readString(p, StandardCharsets.UTF_8);
                    String line = raw.lines()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                        .findFirst().orElse("");
                    if (line.isEmpty()) {
                        LOG.warn("RegionServer Public Key Datei leer: {}", p.toAbsolutePath());
                        continue;
                    }
                    if (!isBase64(line)) {
                        LOG.warn("RegionServer Public Key kein gültiges Base64: {}", p.toAbsolutePath());
                        continue;
                    }
                    LOG.info("RegionServer Public Key Datei gefunden: {} (Base64 Länge {} Zeichen)", p.toAbsolutePath(), line.length());
                    return Optional.of(line);
                }
            } catch (IOException e) {
                LOG.warn("Kann RegionServer Public Key Datei '{}' nicht lesen: {}", p.toAbsolutePath(), e.toString());
            }
        }
        LOG.warn("Kein RegionServer Public Key gefunden ({} geprüfte Pfade)", candidates.size());
        return Optional.empty();
    }

    private boolean isBase64(String s) {
        try {
            Base64.getDecoder().decode(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
