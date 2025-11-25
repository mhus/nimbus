package de.mhus.nimbus.tools.demosetup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class DemoSetupApplication implements CommandLineRunner {

    public static final String MAIN_JWT_TOKEN = "main-jwt-token";
    public static final String REGION_SERVER_JWT_TOKEN = "region-server-jwt-token";
    public static final String REGION_JWT_TOKEN = "region-jwt-token";

    private final List<ClientService> services;
    private final ConfidentialDataService confidentialDataService;
    private final SetupUsersRunner setupUsersRunner;
    private final RegionSetupRunner regionSetupRunner;

    private WebClient universeClient; // autorisierter Client
    private WebClient regionClient; // autorisierter Client
    private WebClient initialClient; // für Login ohne Auth
    private String adminToken; // Bearer Token

    @Value("${universe.base-url}")
    private String universeBaseUrl;
    @Value("${region.base-url}")
    private String regionBaseUrl;
    @Value("${region.server.id:local-region-server}")
    private String regionServerId;

    public static void main(String[] args) {
        SpringApplication.run(DemoSetupApplication.class, args);
    }

    @Override
    public void run(String... args) {
        initialClient = WebClient.builder().baseUrl(universeBaseUrl).build();

        int failures = checkServiceHealth();
        if (failures > 0) {
            log.error("Konnte nicht starten, weil Services nicht erreichbar sind.");
            System.exit(1);
        }

        log.info("Starte Demo-Setup CLI Tool - initialisiere Services");
        var adminPassword = confidentialDataService.getUniverseAdminPassword();

        // Admin Login durchführen um Bearer Token zu erhalten
        var adminToken = loginAdmin(adminPassword);
        if (adminToken == null) {
            throw new RuntimeException("Admin Login fehlgeschlagen");
        }
        universeClient = WebClient.builder()
                .baseUrl(universeBaseUrl)
                .defaultHeader("Authorization", "Bearer " + adminToken)
                .build();
        regionClient = WebClient.builder()
                .baseUrl(regionBaseUrl)
                .defaultHeader("Authorization", "Bearer " + adminToken)
                .build();
        log.info("Admin Login erfolgreich – Bearer Token gesetzt.");

        // Region Server Public Key Registrierung NACH Login
        registerRegionServerPublicKeyAtUniverse();
        registerUniverseServerPublicKeyAtRegion();

        // Nutzer sicherstellen (nur wenn Universe konfiguriert)
//        setupUsersRunner.run();
        regionSetupRunner.run();

        failures = checkServiceHealth();
        int exitCode = (failures == 0) ? 0 : 1;
        log.info("Beende Anwendung mit Exit-Code {}", exitCode);
        System.exit(exitCode);
    }

    private void registerRegionServerPublicKeyAtUniverse() {
        var raw = confidentialDataService.getRegionServerPublicKey();
        try {
            var pubOpt = parsePublicKeyFromString(raw);
            if (pubOpt.isEmpty()) {
                log.warn("RegionServer Public Key Format unbekannt / nicht parsbar");
            } else {
                PublicKey pub = pubOpt.get();
                String base64 = Base64.getEncoder().encodeToString(pub.getEncoded());
                log.info("RegionServer Public Key geladen (Algorithmus={}, Länge={} Base64-Zeichen)", pub.getAlgorithm(), base64.length());
                boolean exists = existsKeyInUniverse("REGION","PUBLIC", regionServerId, REGION_SERVER_JWT_TOKEN);
                if (!exists) {
                    createKeyInUniverse("REGION","PUBLIC", pub.getAlgorithm(), regionServerId, REGION_SERVER_JWT_TOKEN, regionServerId + ":" + REGION_SERVER_JWT_TOKEN + ":" + java.util.UUID.randomUUID(), base64);
                } else {
                    log.debug("RegionServer Public Key bereits registriert (owner={}, intent={})", regionServerId, REGION_SERVER_JWT_TOKEN);
                }
            }
        } catch (Exception e) {
            log.warn("Fehler beim Verarbeiten des RegionServer Public Keys: {}", e.toString());
        }
    }

    private void registerUniverseServerPublicKeyAtRegion() {
        var raw = confidentialDataService.getUniverseServerPublicKey();
        try {
            var pubOpt = parsePublicKeyFromString(raw);
            if (pubOpt.isEmpty()) {
                log.warn("UniverseServer Public Key Format unbekannt / nicht parsbar");
            } else {
                PublicKey pub = pubOpt.get();
                String base64 = Base64.getEncoder().encodeToString(pub.getEncoded());
                log.info("RegionServer Public Key geladen (Algorithmus={}, Länge={} Base64-Zeichen)", pub.getAlgorithm(), base64.length());
                boolean exists = existsKeyInRegion("UNIVERSE","PUBLIC", regionServerId, MAIN_JWT_TOKEN);
                if (!exists) {
                    createKeyInRegion("UNIVERSE","PUBLIC", pub.getAlgorithm(), regionServerId, MAIN_JWT_TOKEN, regionServerId + ":" + MAIN_JWT_TOKEN + ":" + java.util.UUID.randomUUID(), base64);
                } else {
                    log.debug("RegionServer Public Key bereits registriert (owner={}, intent={})", regionServerId, MAIN_JWT_TOKEN);
                }
            }
        } catch (Exception e) {
            log.warn("Fehler beim Verarbeiten des RegionServer Public Keys: {}", e.toString());
        }
    }

    private String loginAdmin(String password) {
        try {
            var body = java.util.Map.of(
                    "username", "admin",
                    "password", password
            );
            var resp = initialClient.post()
                    .uri("/universe/user/auth/login")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();
            if (resp == null) {
                log.warn("Login Antwort leer");
                return null;
            }
            // Erwartete Felder: token / bearer / accessToken (heuristik)
            for (String key : java.util.List.of("token", "bearer", "accessToken", "jwt")) {
                Object v = resp.get(key);
                if (v instanceof String s && !s.isBlank()) {
                    return s.trim();
                }
            }
            log.warn("Kein Token Feld in Login Response gefunden: {}", resp.keySet());
            return null;
        } catch (Exception e) {
            log.warn("Admin Login fehlgeschlagen: {}", e.toString());
            return null;
        }
    }

    private int checkServiceHealth() {
        int failures = 0;
        for (ClientService service : services) {
            String name = service.getName();
            if (!service.isConfigured()) {
                log.warn("Service '{}' nicht konfiguriert - überspringe", name);
                continue;
            }
            log.info("Prüfe Service '{}' ...", name);
            boolean ok = service.check();
            if (ok) {
                log.info("Service '{}' OK", name);
            } else {
                log.error("Service '{}' FEHLER", name);
                failures++;
            }
        }
        if (failures == 0) {
            log.info("Alle konfigurierten Services erfolgreich geprüft.");
        } else {
            log.error("{} Service(s) mit Fehlern.", failures);
        }
        return failures;
    }

    private java.util.Optional<PublicKey> parsePublicKeyFromString(String raw) {
        try {
            if (raw == null || raw.isBlank()) return java.util.Optional.empty();
            String trimmed = raw.trim();
            String base64;
            if (trimmed.contains("BEGIN PUBLIC KEY") && trimmed.contains("END PUBLIC KEY")) {
                StringBuilder sb = new StringBuilder();
                boolean inside = false;
                for (String line : trimmed.split("\\R")) {
                    line = line.trim();
                    if (line.startsWith("-----BEGIN") && line.contains("PUBLIC KEY")) { inside = true; continue; }
                    if (line.startsWith("-----END") && line.contains("PUBLIC KEY")) { break; }
                    if (inside && !line.isEmpty() && !line.startsWith("#")) sb.append(line);
                }
                base64 = sb.toString();
            } else {
                // entferne Kommentare und Leerzeilen, falls mehrere Zeilen Base64
                base64 = java.util.Arrays.stream(trimmed.split("\\R"))
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                        .reduce("", (a,b) -> a + b);
            }
            byte[] der = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            try { return java.util.Optional.of(KeyFactory.getInstance("EC").generatePublic(spec)); } catch (Exception ignore) {}
            try { return java.util.Optional.of(KeyFactory.getInstance("RSA").generatePublic(spec)); } catch (Exception ignore) {}
            return java.util.Optional.empty();
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private boolean existsKeyInUniverse(String type, String kind, String owner, String intent) {
        try {
            var resp = universeClient.get().uri(uriBuilder -> uriBuilder.path("/shared/key/exists")
                    .queryParam("type", type)
                    .queryParam("kind", kind)
                    .queryParam("owner", owner)
                    .queryParam("intent", intent)
                    .build())
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();
            if (resp == null) return false;
            Object v = resp.get("exists");
            return Boolean.TRUE.equals(v) || (v instanceof Boolean b && b);
        } catch (Exception e) {
            log.warn("existsKeyInUniverse Fehler: {}", e.toString());
            return false;
        }
    }

    private boolean existsKeyInRegion(String type, String kind, String owner, String intent) {
        try {
            var resp = regionClient.get().uri(uriBuilder -> uriBuilder.path("/shared/key/exists")
                            .queryParam("type", type)
                            .queryParam("kind", kind)
                            .queryParam("owner", owner)
                            .queryParam("intent", intent)
                            .build())
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();
            if (resp == null) return false;
            Object v = resp.get("exists");
            return Boolean.TRUE.equals(v) || (v instanceof Boolean b && b);
        } catch (Exception e) {
            log.warn("existsKeyInUniverse Fehler: {}", e.toString());
            return false;
        }
    }

    private void createKeyInUniverse(String type, String kind, String algorithm, String owner, String intent, String name, String base64Key) {
        try {
            var req = new java.util.HashMap<String,Object>();
            req.put("type", type);
            req.put("kind", kind);
            req.put("algorithm", algorithm);
            req.put("owner", owner);
            req.put("intent", intent);
            req.put("name", name);
            req.put("key", base64Key);
            universeClient.post().uri("/shared/key")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            log.info("Key in Universe angelegt: owner={} intent={} kind={} type={}", owner, intent, kind, type);
        } catch (Exception e) {
            log.error("Fehler beim Anlegen des Keys in Universe: {}", e.toString());
        }
    }

    private void createKeyInRegion(String type, String kind, String algorithm, String owner, String intent, String name, String base64Key) {
        try {
            var req = new java.util.HashMap<String,Object>();
            req.put("type", type);
            req.put("kind", kind);
            req.put("algorithm", algorithm);
            req.put("owner", owner);
            req.put("intent", intent);
            req.put("name", name);
            req.put("key", base64Key);
            regionClient.post().uri("/shared/key")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            log.info("Key in Universe angelegt: owner={} intent={} kind={} type={}", owner, intent, kind, type);
        } catch (Exception e) {
            log.error("Fehler beim Anlegen des Keys in Universe: {}", e.toString());
        }
    }
}
