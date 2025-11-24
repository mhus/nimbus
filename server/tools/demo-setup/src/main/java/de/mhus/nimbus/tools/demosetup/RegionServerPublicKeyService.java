package de.mhus.nimbus.tools.demosetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service zum Laden des Region Server Public Keys (regionServerPublicKey.txt).
 * Analog zu {@link AdminService}: konfigurierbarer Pfad (Property + ENV), optionaler Strict-Modus
 * sowie Caching nach dem ersten erfolgreichen Laden.
 *
 * Konfiguration:
 *  - Spring Property: region.public.key.file (Pfad zur Datei)
 *  - Spring Property: region.public.key.strict (true/false für Deaktivierung der Fallback-Suche)
 *  - Environment Variable: REGION_PUBLIC_KEY_FILE (überschreibt optional den Pfad)
 *
 * Fallback-Pfade (wenn strict=false):
 *  confidential/regionServerPublicKey.txt sowie relative Varianten ../, ../../, ../../../
 *
 * Dateiformat:
 *  - Beliebiger Text; leere Zeilen und Kommentarzeilen (# am Anfang) werden entfernt.
 *  - Mehrzeilige Inhalte (z.B. PEM) werden nach dem Filtern wieder mit '\n' verbunden.
 *
 * Caching:
 *  - Bei erstem erfolgreichen Laden wird der Schlüssel im Speicher gehalten.
 *  - Bei nicht gefundenem Schlüssel bleibt der Cache leer und spätere Aufrufe versuchen erneut zu laden.
 */
@Service
public class RegionServerPublicKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(RegionServerPublicKeyService.class);

    private final String configuredPath;
    private final boolean strict;
    private String cachedKey = null;

    public RegionServerPublicKeyService(@Value("${region.public.key.file:}") String configuredPath,
                                        @Value("${region.public.key.strict:false}") boolean strict) {
        this.configuredPath = configuredPath == null ? "" : configuredPath.trim();
        this.strict = strict;
    }

    /**
     * Liefert den Public Key (optional) und cached das Ergebnis beim ersten erfolgreichen Laden.
     */
    public Optional<String> getRegionServerPublicKey() {
        if (cachedKey != null) return Optional.of(cachedKey);
        Optional<String> key = loadKey();
        cachedKey = key.orElse(null); // null bedeutet: später nochmal versuchen erlaubt
        return key;
    }

    /**
     * Liefert den Public Key in einer Base64-geeigneten kompakten Darstellung.
     * Falls die geladene Datei ein PEM-Format mit BEGIN/END Header hat, werden
     * nur die Zeilen zwischen den Headern (ohne Newlines) zurückgegeben.
     * Andernfalls wird der gesamte Inhalt ohne Whitespaces/Zeilenumbrüche geliefert.
     */
    public Optional<String> getPublicKeyBase64() {
        return getRegionServerPublicKey().map(raw -> {
            if (raw.contains("BEGIN PUBLIC KEY") && raw.contains("END PUBLIC KEY")) {
                String[] lines = raw.split("\\R");
                StringBuilder sb = new StringBuilder();
                boolean inside = false;
                for (String line : lines) {
                    if (line.contains("BEGIN PUBLIC KEY")) { inside = true; continue; }
                    if (line.contains("END PUBLIC KEY")) { break; }
                    if (inside) sb.append(line.trim());
                }
                String pemPart = sb.toString().trim();
                if (!pemPart.isEmpty()) return pemPart; // Rückgabe des PEM Base64 Inhalts
            }
            // Kein PEM: entferne alle Whitespaces / Newlines
            return raw.replaceAll("\\s+", "");
        });
    }

    private Optional<String> loadKey() {
        List<Path> candidates = new ArrayList<>();
        String env = System.getenv("REGION_PUBLIC_KEY_FILE");
        if (!configuredPath.isBlank()) candidates.add(Path.of(configuredPath));
        if (env != null && !env.isBlank()) candidates.add(Path.of(env.trim()));

        if (!strict) {
            candidates.add(Path.of("confidential/regionServerPublicKey.txt"));
            candidates.add(Path.of("../confidential/regionServerPublicKey.txt"));
            candidates.add(Path.of("../../confidential/regionServerPublicKey.txt"));
            candidates.add(Path.of("../../../confidential/regionServerPublicKey.txt"));
        } else {
            LOG.debug("Strict-Modus aktiv: verwende nur explizite Pfade und ENV");
        }

        for (Path p : candidates) {
            try {
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    String raw = Files.readString(p, StandardCharsets.UTF_8);
                    List<String> lines = raw.lines()
                            .map(String::trim)
                            .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                            .toList();
                    if (lines.isEmpty()) {
                        LOG.warn("RegionServer Public Key Datei leer: {}", p.toAbsolutePath());
                        continue;
                    }
                    // Für PEM oder Base64 mehrzeilig: wir erhalten die Originalzeilen (getrimmt) und fügen sie wieder zusammen
                    String key = String.join("\n", lines);
                    if (key.isEmpty()) {
                        LOG.warn("RegionServer Public Key Datei enthält nach Filterung keinen Inhalt: {}", p.toAbsolutePath());
                        continue;
                    }
                    LOG.info("RegionServer Public Key Datei gefunden: {} (Schlüssellänge {} Zeichen)", p.toAbsolutePath(), key.length());
                    return Optional.of(key);
                }
            } catch (IOException e) {
                LOG.warn("Kann Datei '{}' nicht lesen: {}", p.toAbsolutePath(), e.toString());
            }
        }
        LOG.warn("Kein RegionServer Public Key gefunden (Dateiname regionServerPublicKey.txt, ENV REGION_PUBLIC_KEY_FILE oder Property region.public.key.file)");
        return Optional.empty();
    }
}
