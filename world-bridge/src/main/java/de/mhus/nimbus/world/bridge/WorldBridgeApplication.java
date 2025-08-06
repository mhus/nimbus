package de.mhus.nimbus.world.bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class WorldBridgeApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WorldBridgeApplication.class, args);
    }
}
