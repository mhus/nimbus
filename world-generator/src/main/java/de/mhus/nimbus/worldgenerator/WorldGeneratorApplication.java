package de.mhus.nimbus.worldgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(
    scanBasePackages = {
        "de.mhus.nimbus.worldgenerator",
        "de.mhus.nimbus.world.shared"
    }
)
public class WorldGeneratorApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WorldGeneratorApplication.class, args);
    }

}
