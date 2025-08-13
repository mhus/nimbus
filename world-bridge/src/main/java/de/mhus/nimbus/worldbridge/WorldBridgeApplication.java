package de.mhus.nimbus.worldbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class WorldBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldBridgeApplication.class, args);
    }
}
