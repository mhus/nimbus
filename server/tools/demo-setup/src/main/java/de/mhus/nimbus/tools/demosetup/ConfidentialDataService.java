package de.mhus.nimbus.tools.demosetup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@Slf4j
public class ConfidentialDataService {

    private static Path ROOT_DIR = Path.of("./confidential");

    public String getContent(String file) {
        try {
            return getContent(ROOT_DIR, file);
        } catch (IllegalArgumentException e) {
        }
        try {
            return getContent(ROOT_DIR.getParent(), file);
        } catch (IllegalArgumentException e) {
        }
        try {
            return getContent(ROOT_DIR.getParent().getParent(), file);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public String getContent(Path root, String file) {
        // Validate input
        if (file == null || file.isBlank()) {
            throw new IllegalArgumentException("file name blank");
        }
        var target = ROOT_DIR.resolve(file).normalize();
        try {
            // Read content as UTF-8
            return java.nio.file.Files.readString(target, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            log.warn("Kann vertrauliche Datei nicht lesen: {} - {}", target, e.getMessage());
            throw new IllegalStateException("Cannot read confidential content: " + e.getMessage(), e);
        }
    }

    public String getUniverseAdminPassword() {
        return getContent("universeAdminPassword.txt");
    }

    public String getUniverseServerPublicKey() {
        return getContent("universeServerPublicKey.txt");
    }

    public String getRegionServerPublicKey() {
        return getContent("regionServerPublicKey.txt");
    }

}
