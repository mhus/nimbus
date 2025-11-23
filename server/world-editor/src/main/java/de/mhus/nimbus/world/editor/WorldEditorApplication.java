package de.mhus.nimbus.world.editor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorldEditorApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldEditorApplication.class, args);
    }
}

