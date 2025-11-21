package de.mhus.nimbus.evaluate;

import de.mhus.nimbus.tools.generatets.GenerateTsToJavaMojo;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluateTypesOnlyIT {

    @Test
    public void generateFromTypesAndCompile() throws Exception {
        File pluginModuleBase = new File(System.getProperty("user.dir"));
        File moduleBase = new File(pluginModuleBase, "evaluate").getCanonicalFile();
        assertTrue(new File(moduleBase, "pom.xml").exists(), "evaluate/pom.xml must exist: " + moduleBase);

        Path tsDir = moduleBase.toPath().resolve("ts");
        assertTrue(Files.exists(tsDir), "TS directory must exist: " + tsDir);

        Path targetDir = moduleBase.toPath().resolve("target");
        File outJavaDir = moduleBase.toPath().resolve("src/main/java").toFile();
        File modelFile = new File(targetDir.toFile(), "model.json");

        // clean output
        deleteRecursively(outJavaDir);
        if (modelFile.exists()) assertTrue(modelFile.delete(), "Could not delete previous model file: " + modelFile);
        Files.createDirectories(outJavaDir.toPath());
        Files.createDirectories(targetDir);

        // run mojo with single sourceDir and dedicated config
        GenerateTsToJavaMojo mojo = new GenerateTsToJavaMojo();
        setField(mojo, "sourceDirs", Arrays.asList(tsDir.toFile().getAbsolutePath()));
        setField(mojo, "outputDir", outJavaDir);
        setField(mojo, "modelFile", modelFile);
        setField(mojo, "configFile", new File(moduleBase, "ts-to-java-types.yaml"));
        mojo.execute();

        // assertions
        assertTrue(outJavaDir.exists(), "Output dir not created: " + outJavaDir);
        List<File> javaFiles = collectJavaFiles(outJavaDir);
        assertFalse(javaFiles.isEmpty(), "Expected Java files to be generated from types, but none were found in: " + outJavaDir);
        String expectedPkgPath = "de.mhus.nimbus.evaluate.generated.types".replace('.', File.separatorChar);
        boolean foundInPkg = javaFiles.stream().anyMatch(f -> f.getPath().contains(expectedPkgPath));
        assertTrue(foundInPkg, "Expected generated Java under package 'de.mhus.nimbus.evaluate.generated.types'");

        // verify that additionalClassAnnotations were emitted on at least one generated class
        File anyGenerated = javaFiles.stream().filter(f -> f.getPath().contains(expectedPkgPath)).findFirst().orElse(null);
        assertNotNull(anyGenerated, "No generated file found in expected package");
        String content = Files.readString(anyGenerated.toPath());
        assertTrue(content.contains("@Deprecated"),
                "Expected additional annotation @Deprecated to be present in generated class");

        // verify field-level annotations configuration
        // Choose the Item class which has required (id, itemType) and optional (name, description, ...) fields
        File itemFile = javaFiles.stream()
                .filter(f -> f.getPath().contains(expectedPkgPath) && f.getName().equals("Item.java"))
                .findFirst().orElse(null);
        assertNotNull(itemFile, "Expected generated Item.java in types package to test field annotations");
        String itemSrc = Files.readString(itemFile.toPath());
        // Common field annotation should be present before fields
        assertTrue(itemSrc.contains("@Deprecated"),
                "Expected common field annotation @Deprecated on fields");
        // Optional field annotations: expect optional-specific annotation for a field such as 'name'
        assertTrue(itemSrc.contains("@SuppressWarnings(\"optional\")"),
                "Expected optional field annotation @SuppressWarnings(\"optional\") on optional fields");
        // @JsonInclude(NON_NULL) is no longer hardcoded; it can be configured via additionalOptionalFieldAnnotations
        // Non-optional field annotation should be present for required fields such as 'id' or 'itemType'
        assertTrue(itemSrc.contains("@SuppressWarnings(\"required\")"),
                "Expected non-optional field annotation @SuppressWarnings(\"required\") on required fields");

        // build evaluate via maven
        int exit = runMaven(moduleBase, "clean", "package", "-DskipTests", "-Dmaven.compiler.release=21");
        assertEquals(0, exit, "Maven build of evaluate module failed");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static List<File> collectJavaFiles(File dir) throws IOException {
        try (var stream = Files.walk(dir.toPath())) {
            return stream.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    private static List<File> listFilesDepthFirst(File dir) throws IOException {
        if (dir == null || !dir.exists()) return java.util.Collections.emptyList();
        try (var stream = Files.walk(dir.toPath())) {
            return stream
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) return;
        if (file.isFile()) {
            if (!file.delete()) throw new IOException("Failed to delete file: " + file);
            return;
        }
        for (File f : listFilesDepthFirst(file)) {
            if (f.isDirectory()) {
                if (!f.delete() && f.exists()) throw new IOException("Failed to delete dir: " + f);
            } else {
                if (!f.delete() && f.exists()) throw new IOException("Failed to delete file: " + f);
            }
        }
    }

    private static int runMaven(File workingDir, String... args) throws IOException, InterruptedException {
        String mvn = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
        String[] cmd = new String[1 + args.length];
        cmd[0] = mvn;
        System.arraycopy(args, 0, cmd, 1, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
        }
        boolean finished = p.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            fail("Maven build timed out in " + workingDir);
        }
        int exit = p.exitValue();
        if (exit != 0) {
            System.out.println("[DEBUG_LOG] Maven output for failure in " + workingDir + ":\n" + out);
        }
        return exit;
    }
}
