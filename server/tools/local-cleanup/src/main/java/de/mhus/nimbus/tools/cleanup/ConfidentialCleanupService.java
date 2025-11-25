package de.mhus.nimbus.tools.cleanup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;

@Service
@Slf4j
public class ConfidentialCleanupService {
    private static final Path ROOT_PATH = Paths.get("./confidential");

    public void cleanup() {
        // Falls Verzeichnis nicht existiert: nichts zu tun
        if (!Files.exists(ROOT_PATH) || !Files.isDirectory(ROOT_PATH)) {
            log.info("Confidential cleanup: Verzeichnis '{}' nicht vorhanden", ROOT_PATH.toAbsolutePath());
            return;
        }
        int deleted = 0;
        int errors = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ROOT_PATH, "*.txt")) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                try {
                    Files.delete(p);
                    deleted++;
                    log.debug("Gelöscht: {}", p.getFileName());
                } catch (Exception e) {
                    errors++;
                    log.warn("Konnte Datei '{}' nicht löschen: {}", p.getFileName(), e.toString());
                }
            }
        } catch (Exception e) {
            log.error("Fehler beim Auflisten der Dateien in '{}': {}", ROOT_PATH.toAbsolutePath(), e.toString());
            return;
        }
        log.info("Confidential cleanup abgeschlossen: {} .txt Dateien gelöscht, {} Fehler", deleted, errors);
    }
}
