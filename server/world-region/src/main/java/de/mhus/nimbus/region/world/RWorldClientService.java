package de.mhus.nimbus.region.world;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Client zum Aufruf des World-Servers basierend auf einer {@link RWorld} Konfiguration.
 * Endpunkte:
 *  GET  /world/region/world?worldId=...
 *  POST /world/region/{regionId}/session
 */
@Service
public class RWorldClientService {

    private final RestTemplate restTemplate;

    public RWorldClientService() { this.restTemplate = new RestTemplate(); }
    public RWorldClientService(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public record WorldInfoDto(String worldId, Boolean enabled, String parent, String branch) {}
    public record SessionResponse(String id, String status, String expireAt, String worldId, String regionId) {}

    private String base(RWorld w) {
        if (w == null || w.getApiUrl() == null || w.getApiUrl().isBlank()) throw new IllegalArgumentException("world apiUrl missing");
        return w.getApiUrl().replaceAll("/+$", "");
    }

    public Optional<WorldInfoDto> fetchWorld(RWorld world) {
        String url = base(world) + "/world/region/world?worldId=" + world.getWorldId();
        try {
            @SuppressWarnings("unchecked") ResponseEntity<Map<String,Object>> resp = (ResponseEntity) restTemplate.getForEntity(URI.create(url), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<String,Object> b = resp.getBody();
                Boolean enabled = b.get("enabled") instanceof Boolean be ? be : Boolean.TRUE;
                return Optional.of(new WorldInfoDto(
                        (String) b.get("worldId"),
                        enabled,
                        (String) b.get("parent"),
                        (String) b.get("branch")
                ));
            }
            if (resp.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            throw new IllegalStateException("Unexpected status " + resp.getStatusCode());
        } catch (RestClientException e) {
            throw new RuntimeException("World fetch failed: " + e.getMessage(), e);
        }
    }

    public SessionResponse createSession(RWorld world, String regionId, String userId, String characterId) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId blank");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId blank");
        String url = base(world) + "/world/region/" + regionId + "/session";
        try {
            URI uri = URI.create(url + "?worldId=" + encode(world.getWorldId()) + "&userId=" + encode(userId)
                    + (characterId != null ? "&characterId=" + encode(characterId) : ""));
            @SuppressWarnings("unchecked") ResponseEntity<Map<String,Object>> resp = (ResponseEntity) restTemplate.postForEntity(uri, null, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<String,Object> b = resp.getBody();
                return new SessionResponse(
                        (String) b.get("id"),
                        (String) b.get("status"),
                        (String) b.get("expireAt"),
                        (String) b.get("worldId"),
                        (String) b.get("regionId")
                );
            }
            throw new IllegalStateException("Unexpected status " + resp.getStatusCode());
        } catch (RestClientException e) {
            throw new RuntimeException("Session creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Erstellt eine Main World auf dem World Server. Erwartet, dass worldId main ist (kein '$', kein Branch).
     */
    public WorldInfoDto createMainWorld(RWorld world, String name, String description) {
        String wid = world.getWorldId();
        if (wid == null || wid.isBlank()) throw new IllegalArgumentException("worldId blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name blank");
        String url = base(world) + "/world/region/world";
        java.util.Map<String,Object> body = new java.util.LinkedHashMap<>();
        java.util.Map<String,Object> info = new java.util.LinkedHashMap<>();
        info.put("name", name);
        if (description != null) info.put("description", description);
        body.put("worldId", wid);
        body.put("info", info);
        try {
            @SuppressWarnings("unchecked") ResponseEntity<Map<String,Object>> resp = (ResponseEntity) restTemplate.postForEntity(url, body, Map.class);
            if (resp.getStatusCode() == HttpStatus.CREATED && resp.getBody() != null) {
                Map<String,Object> b = resp.getBody();
                Boolean enabled = b.get("enabled") instanceof Boolean be ? be : Boolean.TRUE;
                return new WorldInfoDto((String)b.get("worldId"), enabled, (String)b.get("parent"), (String)b.get("branch"));
            }
            throw new IllegalStateException("Unexpected status " + resp.getStatusCode());
        } catch (RestClientException e) {
            throw new RuntimeException("World create failed: " + e.getMessage(), e);
        }
    }

    private String encode(String v) { return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8); }
}
