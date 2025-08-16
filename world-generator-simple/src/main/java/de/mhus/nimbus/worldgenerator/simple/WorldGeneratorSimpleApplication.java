package de.mhus.nimbus.worldgenerator.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class WorldGeneratorSimpleApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WorldGeneratorSimpleApplication.class, args);
    }
}
