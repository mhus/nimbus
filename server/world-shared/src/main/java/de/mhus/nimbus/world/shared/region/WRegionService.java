package de.mhus.nimbus.world.shared.region;

import de.mhus.nimbus.shared.dto.region.RegionWorldRequest;
import de.mhus.nimbus.shared.dto.region.RegionWorldResponse;
import de.mhus.nimbus.generated.dto.RegionCharacterResponse;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service, mit dem der World-Server bequem auf die Region-REST-Routen
 * unter /region/{regionId}/world/{worldId} zugreifen kann.
 */
@Service
public class WRegionService {

    private final RestTemplate rest;
    private final WRegionProperties props;
    private final JwtService jwtService;
    private final KeyService keyService;

    public WRegionService(RestTemplateBuilder builder, WRegionProperties props, JwtService jwtService, KeyService keyService) {
        this.rest = builder
                .requestFactory(() -> {
                    var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
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
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(worldId));
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
        HttpEntity<RegionWorldRequest> entity = new HttpEntity<>(req, authHeaders(worldId));
        ResponseEntity<RegionWorldResponse> resp = rest.exchange(url, HttpMethod.POST, entity, RegionWorldResponse.class);
        return ensureBody(resp);
    }

    public RegionWorldResponse updateWorld(String regionId, String worldId, RegionWorldRequest req) {
        String url = worldUrl(regionId, worldId);
        HttpEntity<RegionWorldRequest> entity = new HttpEntity<>(req, authHeaders(worldId));
        ResponseEntity<RegionWorldResponse> resp = rest.exchange(url, HttpMethod.PUT, entity, RegionWorldResponse.class);
        return ensureBody(resp);
    }

    public void deleteWorld(String regionId, String worldId) {
        String url = worldUrl(regionId, worldId);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(worldId));
        rest.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public Optional<RegionCharacterResponse> getCharacter(String regionId, String characterId, String worldId) {
        String url = characterUrl(regionId, characterId);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(worldId));
        try {
            ResponseEntity<RegionCharacterResponse> resp = rest.exchange(url, HttpMethod.GET, entity, RegionCharacterResponse.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(resp.getBody());
            }
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------

    private String worldUrl(String regionId, String worldId) {
        String base = props.getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/region/" + regionId + "/world/" + worldId;
    }

    private String characterUrl(String regionId, String characterId) {
        String base = props.getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/region/world/character/" + regionId + "/" + characterId;
    }

    private HttpHeaders authHeaders(String worldId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(createWorldToken(worldId));
        return headers;
    }

    private String createWorldToken(String worldId) {
        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.WORLD, KeyIntent.of(worldId, KeyIntent.MAIN_JWT_TOKEN));
        if (privateKeyOpt.isEmpty()) {
            throw new IllegalStateException("Kein PrivateKey für WORLD owner=" + worldId + " gefunden");
        }
        Instant exp = Instant.now().plus(Duration.ofMinutes(5));
        return jwtService.createTokenWithSecretKey(privateKeyOpt.get(), worldId, null, exp);
    }

    private static <T> T ensureBody(ResponseEntity<T> resp) {
        if (resp == null || resp.getBody() == null) {
            throw new IllegalStateException("Leere Antwort vom Region-Server");
        }
        return resp.getBody();
    }
}
