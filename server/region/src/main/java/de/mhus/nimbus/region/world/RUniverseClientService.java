package de.mhus.nimbus.region.world;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client zum Universe-Server für Welt-Erstellung / Abfrage über Region.
 * Endpunkte:
 *  GET  /universe/region/{regionId}/world/{worldId}
 *  POST /universe/region/{regionId}/world/{worldId}
 */
@Service
public class RUniverseClientService {

    private final RestTemplate restTemplate;
    public RUniverseClientService() { this.restTemplate = new RestTemplate(); }
    public RUniverseClientService(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public record UniverseWorldDto(String id, String name, String description, String regionId, String worldId, String coordinates) {}

    private String baseUrl; // konfigurierbar per Setter oder später aus Properties laden

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    private String base() { if (baseUrl == null || baseUrl.isBlank()) throw new IllegalStateException("Universe baseUrl not set"); return baseUrl.replaceAll("/+$", ""); }

    public Optional<UniverseWorldDto> fetch(String regionId, String worldId) {
        String url = base() + "/universe/region/" + encode(regionId) + "/world/" + encode(worldId);
        try {
            @SuppressWarnings("unchecked") ResponseEntity<Map<String,Object>> resp = (ResponseEntity) restTemplate.getForEntity(URI.create(url), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<String,Object> b = resp.getBody();
                return Optional.of(new UniverseWorldDto(
                        (String)b.get("id"),
                        (String)b.get("name"),
                        (String)b.get("description"),
                        (String)b.get("regionId"),
                        (String)b.get("worldId"),
                        (String)b.get("coordinates")
                ));
            }
            if (resp.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            throw new IllegalStateException("Unexpected status " + resp.getStatusCode());
        } catch (RestClientException e) { throw new RuntimeException("Universe world fetch failed: " + e.getMessage(), e); }
    }

    public UniverseWorldDto create(String regionId, String worldId, String name, String description, String coordinates) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId blank");
        if (worldId == null || worldId.isBlank()) throw new IllegalArgumentException("worldId blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name blank");
        String url = base() + "/universe/region/" + encode(regionId) + "/world/" + encode(worldId);
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (description != null) body.put("description", description);
        if (coordinates != null) body.put("coordinates", coordinates);
        try {
            @SuppressWarnings("unchecked") ResponseEntity<Map<String,Object>> resp = (ResponseEntity) restTemplate.postForEntity(url, body, Map.class);
            if (resp.getStatusCode() == HttpStatus.CREATED && resp.getBody() != null) {
                Map<String,Object> b = resp.getBody();
                return new UniverseWorldDto(
                        (String)b.get("id"),
                        (String)b.get("name"),
                        (String)b.get("description"),
                        (String)b.get("regionId"),
                        (String)b.get("worldId"),
                        (String)b.get("coordinates")
                );
            }
            throw new IllegalStateException("Unexpected status " + resp.getStatusCode());
        } catch (RestClientException e) { throw new RuntimeException("Universe world create failed: " + e.getMessage(), e); }
    }

    private String encode(String v) { return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8); }
}

