package de.mhus.nimbus.tools.demosetup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class UniverseClientService extends BaseClientService {
    private volatile String token;

    public UniverseClientService(@Value("${universe.base-url:}") String baseUrl) {
        super(baseUrl);
    }
    @Override
    public String getName() { return "universe"; }

    public boolean loginAdmin(String password) {
        if (!isConfigured()) return false;
        try {
            Mono<String> mono = super.webClient.post()
                    .uri("/universe/user/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"admin\",\"password\":\"" + password + "\"}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        LOG.error("Admin Login Fehler: {}", e.toString());
                        return Mono.empty();
                    });
            String body = mono.block();
            if (body == null) {
                LOG.error("Admin Login Antwort leer");
                return false;
            }
            // primitive Token Extraktion
            int pos = body.indexOf("\"token\":");
            if (pos >= 0) {
                int start = body.indexOf('"', pos + 8);
                int end = body.indexOf('"', start + 1);
                if (start >= 0 && end > start) {
                    String t = body.substring(start + 1, end);
                    if (!t.isBlank()) {
                        token = t;
                        LOG.info("Admin Token erhalten ({} Zeichen)", t.length());
                        return true;
                    }
                }
            }
            LOG.error("Token nicht im Login-Body gefunden");
            return false;
        } catch (Exception e) {
            LOG.error("Exception beim Admin Login: {}", e.toString());
            return false;
        }
    }

    public boolean hasToken() { return token != null && !token.isBlank(); }

    public boolean userExists(String username) {
        if (!isConfigured() || !hasToken()) return false; // ohne Token keine Prüfung
        try {
            String body = super.webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/universum/user/user").queryParam("username", username).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        LOG.warn("Fehler userExists('{}'): {}", username, e.toString());
                        return Mono.empty();
                    })
                    .block();
            if (body == null) return false;
            return body.contains("\"username\":\"" + username + "\"");
        } catch (Exception e) {
            LOG.warn("Exception userExists('{}'): {}", username, e.toString());
            return false;
        }
    }

    public boolean createUser(String username, String password) {
        if (!isConfigured() || !hasToken()) return false;
        try {
            String email = username + "@example.org"; // einfache Ableitung
            String json = "{" +
                    "\"username\":\"" + username + "\"," +
                    "\"email\":\"" + email + "\"," +
                    "\"roles\":\"USER\"," +
                    "\"password\":\"" + password + "\"}";
            Integer status = super.webClient.post()
                    .uri("/universum/user/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(json)
                    .retrieve()
                    .toBodilessEntity()
                    .map(r -> r.getStatusCode().value())
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        LOG.error("Fehler createUser('{}'): {}", username, e.toString());
                        return Mono.just(-1);
                    })
                    .block();
            if (status != null && (status == 200 || status == 201)) {
                LOG.info("User '{}' angelegt (Status {})", username, status);
                return true;
            } else {
                LOG.error("User '{}' nicht angelegt (Status {})", username, status);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception createUser('{}'): {}", username, e.toString());
            return false;
        }
    }

    public void ensureUser(String username, String password) {
        if (!isConfigured()) {
            LOG.warn("Universe nicht konfiguriert - kann User '{}' nicht prüfen", username);
            return;
        }
        if (!hasToken()) {
            LOG.warn("Kein Token - erst Admin Login nötig für User '{}'", username);
            return;
        }
        if (userExists(username)) {
            LOG.info("User '{}' existiert bereits - überspringe", username);
        } else {
            LOG.info("User '{}' existiert nicht - lege an", username);
            createUser(username, password);
        }
    }

    public boolean regionExists(String name) {
        if (!isConfigured() || !hasToken()) return false;
        try {
            String body = webClient.get()
                    .uri("/universe/region")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        LOG.warn("Fehler regionExists('{}'): {}", name, e.toString());
                        return Mono.empty();
                    })
                    .block();
            if (body == null) return false;
            return body.contains("\"name\":\"" + name + "\"");
        } catch (Exception e) {
            LOG.warn("Exception regionExists('{}'): {}", name, e.toString());
            return false;
        }
    }

    public boolean createRegion(String name, String apiUrl, String publicSignKey) {
        if (!isConfigured() || !hasToken()) return false;
        try {
            String maintainers = "admin"; // Standard: Admin als Maintainer
            String json = "{" +
                    "\"name\":\"" + name + "\"," +
                    "\"apiUrl\":\"" + apiUrl + "\"," +
                    "\"publicSignKey\":\"" + (publicSignKey == null ? "" : publicSignKey) + "\"," +
                    "\"maintainers\":\"" + maintainers + "\"}";
            Integer status = webClient.post()
                    .uri("/universe/region")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(json)
                    .retrieve()
                    .toBodilessEntity()
                    .map(r -> r.getStatusCode().value())
                    .timeout(Duration.ofSeconds(8))
                    .onErrorResume(e -> {
                        LOG.error("Fehler createRegion('{}'): {}", name, e.toString());
                        return Mono.just(-1);
                    })
                    .block();
            if (status != null && status == 201) {
                LOG.info("Region '{}' angelegt (Status {})", name, status);
                return true;
            } else {
                LOG.error("Region '{}' nicht angelegt (Status {})", name, status);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception createRegion('{}'): {}", name, e.toString());
            return false;
        }
    }

    public void ensureRegion(String name, String apiUrl, String publicSignKey) {
        if (!isConfigured()) {
            LOG.warn("Universe nicht konfiguriert - kann Region '{}' nicht prüfen", name);
            return;
        }
        if (!hasToken()) {
            LOG.warn("Kein Token - erst Admin Login nötig für Region '{}'");
            return;
        }
        if (regionExists(name)) {
            LOG.info("Region '{}' existiert bereits - überspringe Anlage", name);
        } else {
            LOG.info("Region '{}' existiert nicht - lege an", name);
            createRegion(name, apiUrl, publicSignKey);
        }
    }

    public boolean checkRegionPublicKey(String regionName) {
        if (!isConfigured() || !hasToken()) return false;
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/universe/user/keys")
                            .queryParam("type", "REGION")
                            .queryParam("kind", "PUBLIC")
                            .queryParam("name", regionName)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        LOG.warn("Fehler checkRegionPublicKey('{}'): {}", regionName, e.toString());
                        return Mono.empty();
                    })
                    .block();
            if (body == null) return false;
            boolean found = body.contains("\"keyId\":\"" + regionName + "\"");
            if (found) {
                LOG.info("Public Key für Region '{}' gefunden", regionName);
            } else {
                LOG.warn("Public Key für Region '{}' NICHT gefunden", regionName);
            }
            return found;
        } catch (Exception e) {
            LOG.warn("Exception checkRegionPublicKey('{}'): {}", regionName, e.toString());
            return false;
        }
    }
}
