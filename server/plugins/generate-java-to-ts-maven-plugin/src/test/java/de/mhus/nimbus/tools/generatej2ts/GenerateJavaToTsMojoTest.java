package de.mhus.nimbus.tools.generatej2ts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Integrationstest: Startet das Mojo gegen Beispiel-Java-Dateien und prüft, dass TS-Dateien generiert werden.
 */
public class GenerateJavaToTsMojoTest {

    private Path outDir;

    @AfterEach
    void cleanup() throws IOException {
        if (outDir != null && Files.exists(outDir)) {
            // nicht rekursiv löschen, nur für wiederholte Läufe aufräumen
        }
    }

    @Test
    void generateTypeScriptFromSampleJava() throws Exception {
        File inputDir = locateResourceDir("java2ts/input");
        Assertions.assertTrue(inputDir.isDirectory(), "Input-Verzeichnis nicht gefunden: " + inputDir);

        outDir = Path.of("target", "test-output", "java2ts").toAbsolutePath();
        Files.createDirectories(outDir);

        GenerateJavaToTsMojo mojo = new GenerateJavaToTsMojo();
        setPrivateField(mojo, "inputDirectory", inputDir);
        setPrivateField(mojo, "outputDirectory", outDir.toFile());
        // optional: Konfiguration ist nicht erforderlich; auf nicht existente Datei setzen
        setPrivateField(mojo, "configFile", new File("target/java-to-ts-test.yaml"));

        // execute
        mojo.execute();

        // verify files
        Path personTs = outDir.resolve(Path.of("models", "Person.ts"));
        Path addressTs = outDir.resolve(Path.of("models", "Address.ts"));
        Path statusTs = outDir.resolve(Path.of("enums", "Status.ts"));
        Assertions.assertTrue(Files.exists(personTs), "Person.ts wurde nicht erzeugt");
        Assertions.assertTrue(Files.exists(addressTs), "Address.ts wurde nicht erzeugt");
        Assertions.assertTrue(Files.exists(statusTs), "Status.ts wurde nicht erzeugt");

        // content checks
        String person = Files.readString(personTs, StandardCharsets.UTF_8);
        // Header mit Source-FQN prüfen
        Assertions.assertTrue(person.contains("Source: de.example.models.Person"), "Header enthält nicht die Java Source FQN");
        Assertions.assertTrue(person.contains("export interface Person"));
        Assertions.assertTrue(person.contains("name: string;"), "name: string fehlt");
        Assertions.assertTrue(person.contains("age?: number;"), "age?: number fehlt (optional)");
        Assertions.assertTrue(person.contains("age?: number; /* age in years */"), "Beschreibungskommentar fehlt oder falsch");
        // Klassen-Import aus @TypeScriptImport
        Assertions.assertTrue(person.contains("import { ColorHex } from '../types/ColorHex';"), "Import wurde nicht übernommen");

        String status = Files.readString(statusTs, StandardCharsets.UTF_8);
        Assertions.assertTrue(status.contains("export enum Status"));
        Assertions.assertTrue(status.contains("ACTIVE"));
        Assertions.assertTrue(status.contains("INACTIVE"));
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static File locateResourceDir(String rel) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(rel);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + rel);
        }
        return new File(url.getFile());
    }
}
