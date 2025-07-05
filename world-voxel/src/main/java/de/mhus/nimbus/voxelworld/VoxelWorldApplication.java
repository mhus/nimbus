package de.mhus.nimbus.voxelworld;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VoxelWorld Service Application
 * Spring Boot Anwendung für das VoxelWorld-Modul
 */
@Slf4j
@SpringBootApplication
public class VoxelWorldApplication {

    public static void main(String[] args) {
        LOGGER.info("Starting VoxelWorld Application...");
        SpringApplication.run(VoxelWorldApplication.class, args);
        LOGGER.info("VoxelWorld Application started successfully");
    }
}
