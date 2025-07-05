package de.mhus.nimbus.voxelworld.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * VoxelWorld REST Controller
 * Stellt HTTP-Endpunkte für VoxelWorld-Funktionalitäten bereit
 */
@Slf4j
@RestController
@RequestMapping("/api/voxelworld")
public class VoxelWorldController {

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        LOGGER.debug("Getting VoxelWorld status");
        Map<String, Object> status = Map.of(
                "status", "active",
                "world", "voxelworld-main",
                "timestamp", System.currentTimeMillis()
        );
        LOGGER.info("VoxelWorld status requested: {}", status.get("status"));
        return status;
    }

    @GetMapping("/info")
    public Map<String, Object> getWorldInfo() {
        LOGGER.debug("Getting VoxelWorld info");
        Map<String, Object> info = Map.of(
                "worldSize", 1000,
                "chunkSize", 16,
                "maxPlayers", 100,
                "currentPlayers", 0
        );
        LOGGER.info("VoxelWorld info requested - worldSize: {}, maxPlayers: {}",
                   info.get("worldSize"), info.get("maxPlayers"));
        return info;
    }
}
