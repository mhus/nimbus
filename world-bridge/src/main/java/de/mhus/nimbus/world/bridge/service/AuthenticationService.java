package de.mhus.nimbus.world.bridge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class AuthenticationService {

    @Value("${nimbus.identity.service.url:http://localhost:8080}")
    private String identityServiceUrl;

    @Value("${nimbus.world.shared.secret:default-secret}")
    private String sharedSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public AuthenticationResult validateToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("X-Shared-Secret", sharedSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                identityServiceUrl + "/api/auth/validate",
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return new AuthenticationResult(
                    true,
                    (String) body.get("userId"),
                    Set.copyOf((Set<String>) body.getOrDefault("roles", Set.of())),
                    (String) body.get("username")
                );
            }

        } catch (Exception e) {
            log.error("Error validating token", e);
        }

        return new AuthenticationResult(false, null, Set.of(), null);
    }
}
