package de.mhus.nimbus.tools.generatej2ts;

import org.apache.maven.plugin.logging.Log;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Minimale Konfigurationsklasse für das Java→TS-Plugin.
 *
 * Aktuell werden die Daten generisch als Map geladen. Die Struktur kann
 * später mit echten Feldern (z. B. include/exclude-Regeln, Typ-Mappings etc.)
 * erweitert werden.
 */
public class Configuration {

    private final Map<String, Object> raw;

    public Configuration(Map<String, Object> raw) {
        this.raw = raw == null ? Collections.emptyMap() : raw;
    }

    public static Configuration loadIfExists(File yamlFile, Log log) throws IOException {
        if (yamlFile == null) return null;
        if (!yamlFile.exists()) {
            if (log != null) log.info("Keine Konfiguration gefunden (optional): " + yamlFile.getAbsolutePath());
            return null;
        }
        try (FileInputStream fis = new FileInputStream(yamlFile)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(fis);
            Map<String, Object> map = (data instanceof Map) ? (Map<String, Object>) data : Collections.emptyMap();
            if (log != null) log.info("Konfiguration geladen aus: " + yamlFile.getAbsolutePath());
            return new Configuration(map);
        }
    }

    /**
     * Liefert eine grobe Anzahl an Regeln/Einträgen in der rohen Map zurück.
     * Dient nur der Information im Log.
     */
    public int countRules() {
        return raw.size();
    }

    public Map<String, Object> getRaw() {
        return raw;
    }
}
