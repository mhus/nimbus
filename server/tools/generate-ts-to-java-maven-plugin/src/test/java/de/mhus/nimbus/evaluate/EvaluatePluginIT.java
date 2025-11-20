package de.mhus.nimbus.evaluate;

import de.mhus.nimbus.tools.generatets.GenerateTsToJavaMojo;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluatePluginIT {

    @Test
    public void generateAndCompile() throws Exception {
        File moduleBase = new File(System.getProperty("user.dir"));
        // Ensure we are in the evaluate module
        assertTrue(new File(moduleBase, "pom.xml").exists(), "Must run inside evaluate module");

        Path tsDir = moduleBase.toPath().resolve("ts");
        assertTrue(Files.exists(tsDir), "TS directory must exist: " + tsDir);

        Path targetDir = moduleBase.toPath().resolve("target");
        Files.createDirectories(targetDir);
        File outJavaDir = targetDir.resolve("generated-sources/test-gen").toFile();
        File modelFile = targetDir.resolve("ts-model.json").toFile();

        // Run Mojo
        GenerateTsToJavaMojo mojo = new GenerateTsToJavaMojo();
        setField(mojo, "sourceDirs", Arrays.asList(tsDir.toString()));
        setField(mojo, "outputDir", outJavaDir);
        setField(mojo, "modelFile", modelFile);
        setField(mojo, "configFile", new File(moduleBase, "ts-to-java.yaml")); // optional, may not exist
        mojo.execute();

        // Validate java sources generated
        assertTrue(outJavaDir.exists(), "Output dir not created");
        List<File> javaFiles = collectJavaFiles(outJavaDir);
        assertFalse(javaFiles.isEmpty(), "No Java files generated in " + outJavaDir);

        // Compile generated sources
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No system JavaCompiler available (are tests running on a JDK?)");

        File classesDir = targetDir.resolve("compiled-classes").toFile();
        if (!classesDir.exists()) assertTrue(classesDir.mkdirs(), "Could not create classes dir");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends javax.tools.JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(javaFiles);
            List<String> options = new ArrayList<>();
            options.addAll(Arrays.asList("-d", classesDir.getAbsolutePath()));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);
            Boolean ok = task.call();
            assertTrue(Boolean.TRUE.equals(ok), "Compilation of generated sources failed");
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static List<File> collectJavaFiles(File dir) throws Exception {
        try (var stream = Files.walk(dir.toPath())) {
            return stream.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }
}
