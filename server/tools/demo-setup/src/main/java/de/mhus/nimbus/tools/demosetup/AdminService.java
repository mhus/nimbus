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

@Service
public class AdminService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminService.class);

    private final String configuredPath;
    private String cachedPassword = null; // ersetzt Optional Cache

    public AdminService(@Value("${admin.file:}") String configuredPath) {
        this.configuredPath = configuredPath == null ? "" : configuredPath.trim();
    }

    public Optional<String> getAdminPassword() {
        if (cachedPassword != null) return Optional.of(cachedPassword);
        Optional<String> pw = loadPassword();
        cachedPassword = pw.orElse(null);
        return pw;
    }

    private Optional<String> loadPassword() {
        List<Path> candidates = new ArrayList<>();

        // 1) explizite Konfiguration über Property admin.file oder Env ADMIN_FILE
        String env = System.getenv("ADMIN_FILE");
        if (!configuredPath.isBlank()) candidates.add(Path.of(configuredPath));
        if (env != null && !env.isBlank()) candidates.add(Path.of(env.trim()));

        // 2) relative Standardpfade (von Modul-Verzeichnis gesehen)
        candidates.add(Path.of("target/admin.txt"));
        candidates.add(Path.of("../target/admin.txt"));
        candidates.add(Path.of("../../target/admin.txt"));
        candidates.add(Path.of("../../../target/admin.txt")); // falls tiefer gestartet

        for (Path p : candidates) {
            try {
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    String raw = Files.readString(p, StandardCharsets.UTF_8);
                    // erste sinnvolle Zeile nehmen
                    String line = raw.lines()
                            .map(String::trim)
                            .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                            .findFirst()
                            .orElse("");
                    if (!line.isEmpty()) {
                        // Format: user:passwort
                        int idx = line.indexOf(':');
                        if (idx <= 0 || idx == line.length() - 1) {
                            LOG.warn("Admin-Passwort Datei ungültiges Format (user:passwort erwartet): {}", p.toAbsolutePath());
                            continue; // versuche nächste Datei
                        }
                        String user = line.substring(0, idx).trim();
                        String pass = line.substring(idx + 1).trim();
                        if (!"admin".equalsIgnoreCase(user)) {
                            LOG.warn("Admin-Passwort Datei Benutzer != 'admin': {} (gefunden '{}')", p.toAbsolutePath(), user);
                            continue; // nächste Datei versuchen
                        }
                        if (pass.isEmpty()) {
                            LOG.warn("Admin-Passwort Datei enthält leeres Passwort: {}", p.toAbsolutePath());
                            continue;
                        }
                        LOG.info("Admin-Passwort Datei gefunden: {} (Passwortlänge {} Zeichen)", p.toAbsolutePath(), pass.length());
                        return Optional.of(pass);
                    } else {
                        LOG.warn("Admin-Passwort Datei leer: {}", p.toAbsolutePath());
                    }
                }
            } catch (IOException e) {
                LOG.warn("Kann Datei '{}' nicht lesen: {}", p, e.toString());
            }
        }
        LOG.warn("Kein Admin-Passwort gefunden (Format user:passwort, user muss 'admin' sein)");
        return Optional.empty();
    }
}
