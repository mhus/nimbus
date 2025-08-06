package de.mhus.nimbus.world.life;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class WorldLifeApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WorldLifeApplication.class, args);
    }
}
