package de.mhus.nimbus.world.terrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "de.mhus.nimbus.world.terrain",
                "de.mhus.nimbus.world.shared"
        }
)
@EnableJpaRepositories
@EnableTransactionManagement
public class WorldTerrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldTerrainApplication.class, args);
    }
}
