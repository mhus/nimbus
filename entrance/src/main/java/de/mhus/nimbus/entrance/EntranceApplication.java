package de.mhus.nimbus.entrance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Hauptanwendungsklasse für das Entrance Modul
 * Stellt WebSocket-Zugang für Client-Authentifizierung und Funktionszugriff bereit
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "de.mhus.nimbus.entrance",
    "de.mhus.nimbus.common"
})
public class EntranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EntranceApplication.class, args);
    }
}
