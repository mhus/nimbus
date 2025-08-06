package de.mhus.nimbus.world.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class WorldDataApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WorldDataApplication.class, args);
    }
}
