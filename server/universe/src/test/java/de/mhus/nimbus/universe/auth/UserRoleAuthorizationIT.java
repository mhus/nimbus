package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUserRepository;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.universe.UniverseProperties;
import de.mhus.nimbus.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UserRoleAuthorizationIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @Autowired
    private UUserService userService;
    @Autowired
    private UUserRepository userRepository;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JwtService jwtService; // optional not directly used but ensures bean context
    @Autowired
    private UniverseProperties jwtProperties; // ensures key config

    private String tokenWithRole;
    private String tokenWithoutRole;

    @BeforeEach
    void setupUsers() {
        // Nutzer mit USER Rolle (Default wird gesetzt im createUser)
        UUser user1 = userService.createUser("withrole","withrole@example.com");
        userService.setPassword(user1.getId(), "pw12345");
        // Nutzer ohne Rolle -> Rollen entfernen und speichern
        UUser user2 = userService.createUser("norole","norole@example.com");
        userService.setPassword(user2.getId(), "pw67890");
        user2.setRoles(); // leeren
        userRepository.save(user2);

        tokenWithRole = login("withrole", "pw12345");
        tokenWithoutRole = login("norole", "pw67890");
        assertNotNull(tokenWithRole, "Token für Nutzer mit Rolle sollte vorhanden sein");
        assertNotNull(tokenWithoutRole, "Token für Nutzer ohne Rolle sollte vorhanden sein");
    }

    private String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        ResponseEntity<ULoginResponse> resp = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), ULoginResponse.class);
        if (resp.getStatusCode() != HttpStatus.OK) return null;
        return Optional.ofNullable(resp.getBody()).map(ULoginResponse::token).orElse(null);
    }

    @Test
    void me_withUserRole_shouldReturn200() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenWithRole);
        ResponseEntity<UMeResponse> resp = restTemplate.exchange("/universe/user/me", HttpMethod.GET, new HttpEntity<>(headers), UMeResponse.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals("withrole", resp.getBody().username());
    }

    @Test
    void me_withoutRole_shouldReturn403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenWithoutRole);
        ResponseEntity<UMeResponse> resp = restTemplate.exchange("/universe/user/me", HttpMethod.GET, new HttpEntity<>(headers), UMeResponse.class);
        assertEquals(403, resp.getStatusCodeValue(), "Erwartet 403 für Nutzer ohne Rolle");
    }
}

