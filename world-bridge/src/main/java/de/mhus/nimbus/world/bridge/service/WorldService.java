package de.mhus.nimbus.world.bridge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorldService {

    @Value("${nimbus.world.terrain.service.url:http://localhost:8081}")
    private String worldTerrainServiceUrl;

    @Value("${nimbus.world.shared.secret:default-secret}")
    private String sharedSecret;

    private final RestTemplate restTemplate;

    public boolean hasWorldAccess(String userId, String worldId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shared-Secret", sharedSecret);
            headers.set("X-User-ID", userId);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                worldTerrainServiceUrl + "/api/worlds/" + worldId + "/access",
                HttpMethod.GET,
                entity,
                Map.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error checking world access for user {} and world {}", userId, worldId, e);
            return false;
        }
    }

    public Object getWorldDetails(String worldId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shared-Secret", sharedSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                worldTerrainServiceUrl + "/api/worlds/" + worldId,
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error getting world details for world {}", worldId, e);
        }

        return null;
    }
}
