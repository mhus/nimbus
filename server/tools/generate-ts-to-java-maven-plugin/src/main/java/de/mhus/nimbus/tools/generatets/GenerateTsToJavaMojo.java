package de.mhus.nimbus.tools.generatets;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * Minimal placeholder Mojo for the generate-ts-to-java Maven plugin.
 * Follows Maven plugin module naming conventions (artifactId ends with -maven-plugin).
 */
@Mojo(name = "generate")
public class GenerateTsToJavaMojo extends AbstractMojo {

    /**
     * List of directories containing TypeScript sources.
     */
    @Parameter(defaultValue = "${project.directory}/ts", property = "sourceDirs")
    private List<String> sourceDirs;

    /**
     * Output directory where generated sources would be placed in the future.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/generate-ts-to-java", property = "outputDir")
    private File outputDir;

    /**
     * Path to the TypeScript model file.
     */
    @Parameter(defaultValue = "${project.build.basedir}/model.json", property = "modelFile")
    private File modelFile;

    @Override
    public void execute() {
        // No-op for now: this is just a placeholder to make the plugin buildable.
        // The actual implementation will transform TypeScript models to Java sources.
        getLog().info("generate-ts-to-java: nothing to generate yet. Using outputDir=" + outputDir);
    }
}
