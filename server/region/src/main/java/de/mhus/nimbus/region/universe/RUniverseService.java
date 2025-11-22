package de.mhus.nimbus.region.universe;

import de.mhus.nimbus.shared.dto.universe.RegionWorldRequest;
import de.mhus.nimbus.shared.dto.universe.RegionWorldResponse;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service, mit dem das Region-Modul bequem auf die Universe-REST-Routen
 * unter /universe/region/{regionId}/world/{worldId} zugreifen kann.
 */
@Service
public class RUniverseService {

    private final RestTemplate rest;
    private final RUniverseProperties props;
    private final JwtService jwtService;
    private final KeyService keyService;

    public RUniverseService(RestTemplateBuilder builder, RUniverseProperties props, JwtService jwtService, KeyService keyService) {
        this.rest = builder
                .requestFactory(() -> {
                    var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                    // Timeouts in ms
                    f.setConnectTimeout(5000);
                    f.setReadTimeout(10000);
                    return f;
                })
                .build();
        this.props = props;
        this.jwtService = jwtService;
        this.keyService = keyService;
    }

    public Optional<RegionWorldResponse> getWorld(String regionId, String worldId) {
        String url = worldUrl(regionId, worldId);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(regionId));
        try {
            ResponseEntity<RegionWorldResponse> resp = rest.exchange(url, HttpMethod.GET, entity, RegionWorldResponse.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(resp.getBody());
            }
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public RegionWorldResponse createWorld(String regionId, String worldId, RegionWorldRequest req) {
        String url = worldUrl(regionId, worldId);
        HttpEntity<RegionWorldRequest> entity = new HttpEntity<>(req, authHeaders(regionId));
        ResponseEntity<RegionWorldResponse> resp = rest.exchange(url, HttpMethod.POST, entity, RegionWorldResponse.class);
        return ensureBody(resp);
    }

    public RegionWorldResponse updateWorld(String regionId, String worldId, RegionWorldRequest req) {
        String url = worldUrl(regionId, worldId);
        HttpEntity<RegionWorldRequest> entity = new HttpEntity<>(req, authHeaders(regionId));
        ResponseEntity<RegionWorldResponse> resp = rest.exchange(url, HttpMethod.PUT, entity, RegionWorldResponse.class);
        return ensureBody(resp);
    }

    public void deleteWorld(String regionId, String worldId) {
        String url = worldUrl(regionId, worldId);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(regionId));
        rest.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    // ------------------------------------------------------------

    private String worldUrl(String regionId, String worldId) {
        String base = props.getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length()-1);
        return base + "/universe/region/" + regionId + "/world/" + worldId;
    }

    private HttpHeaders authHeaders(String regionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(createRegionToken(regionId));
        return headers;
    }

    private String createRegionToken(String regionId) {
        String uuid = props.getRegionKeyUuid();
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalStateException("universe.client.regionKeyUuid ist nicht konfiguriert");
        }
        String keyId = regionId + ":" + uuid.trim();
        // Subjekt beliebig; wir setzen auf die regionId. Kurzlebiges Token (5 Minuten).
        Instant exp = Instant.now().plus(Duration.ofMinutes(5));
        var key = keyService.getLatestPrivateKey(KeyType.UNIVERSE, keyId)
                .orElseThrow(() -> new IllegalStateException("Kein Private Key für Region-Token gefunden: " + keyId));
        return jwtService.createTokenWithSecretKey(key, regionId, null, exp);
    }

    private static <T> T ensureBody(ResponseEntity<T> resp) {
        if (resp == null || resp.getBody() == null) {
            throw new IllegalStateException("Leere Antwort vom Universe-Server");
        }
        return resp.getBody();
    }
}
