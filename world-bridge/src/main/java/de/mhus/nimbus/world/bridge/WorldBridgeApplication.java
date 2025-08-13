package de.mhus.nimbus.world.bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {
        "de.mhus.nimbus.world.bridge",
        "de.mhus.nimbus.world.shared"
})
@EnableKafka
public class WorldBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldBridgeApplication.class, args);
    }
}
