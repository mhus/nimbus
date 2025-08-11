package de.mhus.nimbus.world.terrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {
    "de.mhus.nimbus.world.terrain",
    "de.mhus.nimbus.world.shared"
})
@EnableJpaRepositories
@EnableKafka
public class WorldTerrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldTerrainApplication.class, args);
    }
}
