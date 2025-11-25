package de.mhus.nimbus.tools.demosetup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.security.FormattedKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
public class UniverseClientService extends BaseClientService {
    private volatile String token;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                        log.error("Admin Login Fehler: {}", e.toString());
                        return Mono.empty();
                    });
            String body = mono.block();
            if (body == null) {
                log.error("Admin Login Antwort leer");
                return false;
            }
            try {
                JsonNode node = objectMapper.readTree(body);
                JsonNode tokenNode = node.get("token");
                if (tokenNode != null && !tokenNode.asText().isBlank()) {
                    token = tokenNode.asText();
                    log.info("Admin Token erhalten ({} Zeichen)", token.length());
                    return true;
                }
                log.error("Token Feld nicht gefunden oder leer im Login JSON");
            } catch (Exception parseEx) {
                log.error("Kann Login JSON nicht parsen: {}", parseEx.toString());
            }
            log.error("Token nicht im Login-Body gefunden");
            return false;
        } catch (Exception e) {
            log.error("Exception beim Admin Login: {}", e.toString());
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
                        log.warn("Fehler userExists('{}'): {}", username, e.toString());
                        return Mono.empty();
                    })
                    .block();
            if (body == null) return false;
            return body.contains("\"username\":\"" + username + "\"");
        } catch (Exception e) {
            log.warn("Exception userExists('{}'): {}", username, e.toString());
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
                        log.error("Fehler createUser('{}'): {}", username, e.toString());
                        return Mono.just(-1);
                    })
                    .block();
            if (status != null && (status == 200 || status == 201)) {
                log.info("User '{}' angelegt (Status {})", username, status);
                return true;
            } else {
                log.error("User '{}' nicht angelegt (Status {})", username, status);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception createUser('{}'): {}", username, e.toString());
            return false;
        }
    }

    public void ensureUser(String username, String password) {
        if (!isConfigured()) {
            log.warn("Universe nicht konfiguriert - kann User '{}' nicht prüfen", username);
            return;
        }
        if (!hasToken()) {
            log.warn("Kein Token - erst Admin Login nötig für User '{}'", username);
            return;
        }
        if (userExists(username)) {
            log.info("User '{}' existiert bereits - überspringe", username);
        } else {
            log.info("User '{}' existiert nicht - lege an (Passwortlänge {} Zeichen)", username, password.length());
            boolean created = createUser(username, password);
            if (!created) {
                log.error("Anlage von User '{}' fehlgeschlagen", username);
            }
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
                        log.warn("Fehler regionExists('{}'): {}", name, e.toString());
                        return Mono.empty();
                    })
                    .block();
            if (body == null) return false;
            return body.contains("\"name\":\"" + name + "\"");
        } catch (Exception e) {
            log.warn("Exception regionExists('{}'): {}", name, e.toString());
            return false;
        }
    }

    public boolean createRegion(String name, String apiUrl, FormattedKey publicSignKey) {
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
                        log.error("Fehler createRegion('{}'): {}", name, e.toString());
                        return Mono.just(-1);
                    })
                    .block();
            if (status != null && status == 201) {
                log.info("Region '{}' angelegt (Status {})", name, status);
                return true;
            } else {
                log.error("Region '{}' nicht angelegt (Status {})", name, status);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception createRegion('{}'): {}", name, e.toString());
            return false;
        }
    }

    public void ensureRegion(String name, String apiUrl, FormattedKey publicSignKey) {
        if (!isConfigured()) {
            log.warn("Universe nicht konfiguriert - kann Region '{}' nicht prüfen", name);
            return;
        }
        if (!hasToken()) {
            log.warn("Kein Token - erst Admin Login nötig für Region '{}'");
            return;
        }
        if (regionExists(name)) {
            log.info("Region '{}' existiert bereits - überspringe Anlage", name);
        } else {
            log.info("Region '{}' existiert nicht - lege an", name);
            createRegion(name, apiUrl, publicSignKey);
        }
    }

    public Optional<String> login(String username, String password) {
        if (!isConfigured()) return Optional.empty();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Login Parameter ungültig (username oder password leer)");
            return Optional.empty();
        }
        try {
            String body = webClient.post()
                    .uri("/universe/user/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        log.error("Login Fehler für '{}': {}", username, e.toString());
                        return Mono.empty();
                    })
                    .block();
            if (body == null) {
                log.error("Login Antwort leer für '{}'", username);
                return Optional.empty();
            }
            try {
                JsonNode node = objectMapper.readTree(body);
                JsonNode tokenNode = node.get("token");
                if (tokenNode != null && !tokenNode.asText().isBlank()) {
                    String t = tokenNode.asText();
                    token = t; // aktuelles Token im Service aktualisieren
                    log.info("Login erfolgreich für '{}' (Tokenlänge {} Zeichen)", username, t.length());
                    return Optional.of(t);
                } else {
                    log.error("Token Feld fehlt oder leer im Login JSON für '{}'", username);
                }
            } catch (Exception parseEx) {
                log.error("Kann Login JSON für '{}' nicht parsen: {}", username, parseEx.toString());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Exception beim Login für '{}': {}", username, e.toString());
            return Optional.empty();
        }
    }

}
