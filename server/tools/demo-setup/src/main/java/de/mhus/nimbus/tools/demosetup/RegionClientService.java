package de.mhus.nimbus.tools.demosetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RegionClientService extends BaseClientService {
    private volatile String token;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_PATH = "/region/user/region";
    private static final String CHARACTER_PATH = "/region/user/character";

    public RegionClientService(@Value("${region.base-url:}") String baseUrl) {
        super(baseUrl);
    }
    @Override
    public String getName() { return "region"; }

    public boolean hasToken() { return token != null && !token.isBlank(); }

    public void setToken(String token) { this.token = token; }

    // Prüfen ob Region (Name) bereits lokal registriert ist (Region Server interne Registry)
    public boolean regionExists(String name) {
        if (!isConfigured()) return false;
        try {
            var spec = webClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH).queryParam("name", name).build());
            if (hasToken()) spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            String body = spec.retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> Mono.empty())
                    .block();
            if (body == null || body.isBlank()) return false;
            JsonNode root = mapper.readTree(body);
            if (!root.isArray()) return false;
            return root.elements().hasNext(); // gefilterte Liste mit name param -> any means exists
        } catch (Exception e) {
            return false;
        }
    }

    public boolean createRegion(String name, String apiUrl, String maintainers) {
        if (!isConfigured()) return false;
        try {
            String json = "{" +
                    "\"name\":\"" + name + "\"," +
                    "\"apiUrl\":\"" + apiUrl + "\"," +
                    "\"maintainers\":\"" + (maintainers == null?"":maintainers) + "\"}";
            var spec = webClient.post().uri(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(json);
            if (hasToken()) spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            Integer status = spec.retrieve()
                    .toBodilessEntity()
                    .map(r -> r.getStatusCode().value())
                    .timeout(Duration.ofSeconds(8))
                    .onErrorResume(e -> Mono.just(-1))
                    .block();
            return status != null && (status == 200 || status == 201);
        } catch (Exception e) {
            return false;
        }
    }

    public void ensureRegion(String name, String apiUrl, String maintainers) {
        if (!isConfigured()) return;
        if (regionExists(name)) {
            log.info("Region '{}' lokal bereits vorhanden", name);
        } else {
            log.info("Region '{}' lokal fehlt – lege an", name);
            boolean ok = createRegion(name, apiUrl, maintainers);
            if (!ok) log.warn("Anlage lokale Region '{}' fehlgeschlagen", name);
        }
    }

    // Character Management Methoden
    public boolean characterExists(String userId, String regionId, String characterName) {
        if (!isConfigured()) return false;
        try {
            var spec = webClient.get().uri(CHARACTER_PATH + "/" + regionId + "/list/" + userId);
            if (hasToken()) spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            String body = spec.retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> Mono.empty())
                    .block();
            if (body == null || body.isBlank()) return false;
            JsonNode root = mapper.readTree(body);
            if (!root.isArray()) return false;

            // Prüfe ob Character mit dem Namen bereits existiert
            for (JsonNode character : root) {
                if (character.has("name") && characterName.equals(character.get("name").asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Fehler beim Prüfen ob Character {} existiert: {}", characterName, e.getMessage());
            return false;
        }
    }

    public boolean createCharacter(String userId, String regionId, String characterName, String displayName) {
        if (!isConfigured()) return false;
        try {
            String json = "{" +
                    "\"userId\":\"" + userId + "\"," +
                    "\"regionId\":\"" + regionId + "\"," +
                    "\"name\":\"" + characterName + "\"," +
                    "\"display\":\"" + displayName + "\"}";
            var spec = webClient.post().uri(CHARACTER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(json);
            if (hasToken()) spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            Integer status = spec.retrieve()
                    .toBodilessEntity()
                    .map(r -> r.getStatusCode().value())
                    .timeout(Duration.ofSeconds(8))
                    .onErrorResume(e -> Mono.just(-1))
                    .block();
            if (status != null && (status == 200 || status == 201))
                return true;
            else {
                log.warn("Fehler beim Erstellen von Character {}: Status {}", characterName, status);
            }
        } catch (Exception e) {
            log.warn("Fehler beim Erstellen von Character {}", characterName, e);
        }
        return false;
    }

    public void ensureCharacter(String userId, String regionId, String characterName, String displayName) {
        if (!isConfigured()) return;
        if (characterExists(userId, regionId, characterName)) {
            log.info("Character '{}' für User '{}' in Region '{}' bereits vorhanden", characterName, userId, regionId);
        } else {
            log.info("Character '{}' für User '{}' in Region '{}' fehlt – lege an", characterName, userId, regionId);
            boolean ok = createCharacter(userId, regionId, characterName, displayName);
            if (!ok) log.warn("Anlage Character '{}' für User '{}' fehlgeschlagen", characterName, userId);
        }
    }
}
