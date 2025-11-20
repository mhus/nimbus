package de.mhus.nimbus.tools.generatets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.mhus.nimbus.tools.generatets.ts.TsModel;
import de.mhus.nimbus.tools.generatets.ts.TsParser;
import de.mhus.nimbus.tools.generatets.java.JavaModel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "generate")
public class GenerateTsToJavaMojo extends AbstractMojo {

    /**
     * List of directories containing TypeScript sources.
     */
    @Parameter(defaultValue = "${project.basedir}/ts", property = "sourceDirs")
    private List<String> sourceDirs;

    /**
     * Output directory where generated sources would be placed in the future.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/generate-ts-to-java", property = "outputDir")
    private File outputDir;

    /**
     * Path to the TypeScript model file.
     */
    @Parameter(defaultValue = "${project.basedir}/model.json", property = "modelFile")
    private File modelFile;

    @Parameter(defaultValue = "${project.basedir}/ts-to-java.yaml", property = "configFile")
    private File configFile;

    @Override
    public void execute() {
        try {
            // Load configuration first
            Configuration configuration = loadConfiguration();
            int ignored = configuration.ignoreTsItems == null ? 0 : configuration.ignoreTsItems.size();
            getLog().info("Loaded configuration from " + (configFile == null ? "<none>" : configFile.getPath()) + ": ignoreTsItems=" + ignored);

            TsModel tsModel = parseTs();
            if (tsModel == null) return;

            removeIgnoredItemsFromModel(tsModel, configuration.ignoreTsItems);

            writeModelToFile(tsModel);

            JavaGenerator generator = new JavaGenerator();
            JavaModel javaModel = generator.generate(tsModel);
            getLog().info("Java model created: types=" + (javaModel == null ? 0 : javaModel.getTypes().size()));

            new JavaModelWriter(javaModel).write(outputDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse TypeScript sources", e);
        }
    }

    private void writeModelToFile(TsModel model) throws IOException {
        // Ensure target directory
        if (modelFile == null) {
            getLog().warn("modelFile is not configured; model will not be written.");
            return;
        }
        File parent = modelFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            getLog().warn("Could not create parent directory for model file: " + parent);
        }
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        om.writeValue(modelFile, model);
        getLog().info("Wrote TS model to: " + modelFile.getAbsolutePath() + " (files=" + model.getFiles().size() + ")");
    }

    private void removeIgnoredItemsFromModel(TsModel model, List<String> ignoreTsItems) {
        if (model == null) return;
        if (ignoreTsItems == null || ignoreTsItems.isEmpty()) return;

        java.util.Set<String> ignore = new java.util.HashSet<>(ignoreTsItems);
        int removedTotal = 0;

        if (model.getFiles() != null) {
            for (de.mhus.nimbus.tools.generatets.ts.TsSourceFile f : model.getFiles()) {
                int before = 0;
                int after = 0;

                if (f.getInterfaces() != null) {
                    before += f.getInterfaces().size();
                    f.getInterfaces().removeIf(it -> it != null && it.name != null && ignore.contains(it.name));
                    after += f.getInterfaces().size();
                }
                if (f.getEnums() != null) {
                    before += f.getEnums().size();
                    f.getEnums().removeIf(it -> it != null && it.name != null && ignore.contains(it.name));
                    after += f.getEnums().size();
                }
                if (f.getClasses() != null) {
                    before += f.getClasses().size();
                    f.getClasses().removeIf(it -> it != null && it.name != null && ignore.contains(it.name));
                    after += f.getClasses().size();
                }
                if (f.getTypeAliases() != null) {
                    before += f.getTypeAliases().size();
                    f.getTypeAliases().removeIf(it -> it != null && it.name != null && ignore.contains(it.name));
                    after += f.getTypeAliases().size();
                }
                removedTotal += Math.max(0, before - after);
            }
        }

        if (removedTotal > 0) {
            getLog().info("Ignored TS items removed from model: " + removedTotal);
        } else {
            getLog().info("No TS items matched ignore list (" + ignore.size() + ")");
        }
    }

    private TsModel parseTs() throws IOException {
        List<File> dirs = normalizeSourceDirs();
        if (dirs.isEmpty()) {
            getLog().warn("No sourceDirs available; nothing to parse.");
            return null;
        }
        TsParser parser = new TsParser();
        TsModel model = parser.parse(dirs);
        return model;
    }

    private Configuration loadConfiguration() {
        Configuration c = new Configuration();
        c.ignoreTsItems = Collections.emptyList();
        if (configFile == null) {
            getLog().warn("configFile is not configured; using defaults.");
            return c;
        }
        if (!configFile.exists()) {
            getLog().warn("Config file not found: " + configFile.getAbsolutePath() + "; using defaults.");
            return c;
        }
        try (FileInputStream in = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Configuration loaded = yaml.loadAs(in, Configuration.class);
            if (loaded != null) {
                if (loaded.ignoreTsItems == null) loaded.ignoreTsItems = Collections.emptyList();
                return loaded;
            }
        } catch (Exception e) {
            getLog().error("Failed to load config file '" + configFile + "': " + e.getMessage());
        }
        return c;
    }

    private List<File> normalizeSourceDirs() {
        List<File> result = new ArrayList<>();
        if (sourceDirs != null) {
            for (String s : sourceDirs) {
                if (s == null || s.trim().isEmpty()) continue;
                File f = new File(s);
                if (!f.exists()) {
                    getLog().warn("sourceDir does not exist: " + s);
                    continue;
                }
                result.add(f);
            }
        }
        // Logging summary
        if (!result.isEmpty()) {
            getLog().info("Parsing TS sources from: " + result.stream().map(File::getPath).collect(Collectors.joining(", ")));
        }
        return result;
    }
}
