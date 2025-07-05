package de.mhus.nimbus.voxelworld.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * VoxelWorld REST Controller
 * Stellt HTTP-Endpunkte für VoxelWorld-Funktionalitäten bereit
 */
@RestController
@RequestMapping("/api/voxelworld")
public class VoxelWorldController {

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "status", "active",
                "world", "voxelworld-main",
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/info")
    public Map<String, Object> getWorldInfo() {
        return Map.of(
                "worldSize", 1000,
                "chunkSize", 16,
                "maxPlayers", 100,
                "currentPlayers", 0
        );
    }
}
