package de.mhus.nimbus.world.shared.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client für die Kommunikation mit dem World Terrain Service.
 */
@Component
@Slf4j
public class WorldTerrainClient {

    private final RestTemplate restTemplate;
    private final String worldTerrainServiceUrl;
    private final String sharedSecret;

    public WorldTerrainClient(RestTemplate restTemplate,
                              @Value("${nimbus.service.world-terrain.url:http://localhost:7083}") String worldTerrainServiceUrl,
                              @Value("${nimbus.world.shared.secret}") String sharedSecret) {
        this.restTemplate = restTemplate;
        this.worldTerrainServiceUrl = worldTerrainServiceUrl;
        this.sharedSecret = sharedSecret;
    }

    /**
     * Erstellt eine neue Welt im World Terrain Service.
     */
    public String createWorld(String worldId, String name, String description, int sizeX, int sizeY, Map<String, Object> properties) {
        log.info("Erstelle Welt im World Terrain Service: {} ({}x{})", name, sizeX, sizeY);

        WorldRequest request = new WorldRequest();
        request.setId(worldId);
        request.setName(name);
        request.setDescription(description);
        request.setSizeX(sizeX);
        request.setSizeY(sizeY);
        request.setProperties(properties);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sharedSecret);
        headers.set("Content-Type", "application/json");

        HttpEntity<WorldRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<WorldResponse> response = restTemplate.exchange(
                    worldTerrainServiceUrl + "/api/worlds",
                    HttpMethod.POST,
                    entity,
                    WorldResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Welt erfolgreich erstellt: {}", response.getBody().getId());
                return response.getBody().getId();
            } else {
                throw new RuntimeException("Fehler beim Erstellen der Welt: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Fehler beim Erstellen der Welt: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Erstellen der Welt: " + e.getMessage(), e);
        }
    }

    /**
     * Erstellt Terrain/Map-Daten im World Terrain Service.
     */
    public void createTerrain(Object terrainRequest) {
        log.info("Erstelle Terrain im World Terrain Service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sharedSecret);
        headers.set("Content-Type", "application/json");

        HttpEntity<Object> entity = new HttpEntity<>(terrainRequest, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    worldTerrainServiceUrl + "/api/maps",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Terrain erfolgreich erstellt");
            } else {
                throw new RuntimeException("Fehler beim Erstellen des Terrains: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Fehler beim Erstellen des Terrains: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Erstellen des Terrains: " + e.getMessage(), e);
        }
    }

    // DTOs für die API-Kommunikation
    @Data
    public static class WorldRequest {
        private String id;
        private String name;
        private String description;
        private int sizeX;
        private int sizeY;
        private Map<String, Object> properties;
    }

    @Data
    public static class WorldResponse {
        private String id;
        private String name;
        private String description;
        private int sizeX;
        private int sizeY;
        private Map<String, Object> properties;
        private String createdAt;
        private String updatedAt;
    }
}
