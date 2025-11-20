package de.mhus.nimbus.universe.region;

import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.universe.security.JwtProperties;
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
class RegionAuthorizationIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");
    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) { registry.add("spring.data.mongodb.uri", mongo::getConnectionString); }

    @Autowired private UUserService userService;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JwtService jwtService; // ensure bean
    @Autowired private JwtProperties jwtProperties; // ensure key

    private String adminToken;
    private String maintainerToken;
    private String userToken;

    @BeforeEach
    void setupUsers() {
        UUser admin = userService.createUser("admin","admin@example.com");
        admin.addRole(de.mhus.nimbus.shared.user.UniverseRoles.ADMIN);
        userService.setPassword(admin.getId(), "adminpw");
        UUser maint = userService.createUser("maint","maint@example.com");
        maint.addRole(de.mhus.nimbus.shared.user.UniverseRoles.MAINTAINER);
        userService.setPassword(maint.getId(), "maintpw");
        UUser user = userService.createUser("user","user@example.com");
        // default USER Rolle
        userService.setPassword(user.getId(), "userpw");
        adminToken = login("admin","adminpw");
        maintainerToken = login("maint","maintpw");
        userToken = login("user","userpw");
    }

    private String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        ResponseEntity<de.mhus.nimbus.universe.auth.ULoginResponse> resp = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), de.mhus.nimbus.universe.auth.ULoginResponse.class);
        if (resp.getStatusCode()!=HttpStatus.OK) return null; return Optional.ofNullable(resp.getBody()).map(de.mhus.nimbus.universe.auth.ULoginResponse::token).orElse(null);
    }

    @Test
    void user_cannot_create_Region() {
        HttpHeaders h = bearer(userToken);
        String body = "{\"name\":\"q1\",\"apiUrl\":\"http://api/q1\",\"publicSignKey\":\"key\",\"maintainers\":\"maint\"}";
        ResponseEntity<String> resp = restTemplate.postForEntity("/universe/Region", new HttpEntity<>(body,h), String.class);
        assertEquals(403, resp.getStatusCodeValue());
    }

    @Test
    void maintainer_must_include_self_in_maintainers_for_create() {
        HttpHeaders h = bearer(maintainerToken);
        String bodyMissingSelf = "{\"name\":\"q2\",\"apiUrl\":\"http://api/q2\",\"publicSignKey\":\"key\",\"maintainers\":\"other\"}";
        ResponseEntity<String> resp1 = restTemplate.postForEntity("/universe/Region", new HttpEntity<>(bodyMissingSelf,h), String.class);
        assertEquals(403, resp1.getStatusCodeValue());
        String bodyOk = "{\"name\":\"q3\",\"apiUrl\":\"http://api/q3\",\"publicSignKey\":\"key\",\"maintainers\":\"maint,other\"}";
        ResponseEntity<RegionControllerResponse> resp2 = restTemplate.postForEntity("/universe/Region", new HttpEntity<>(bodyOk,h), RegionControllerResponse.class);
        assertEquals(201, resp2.getStatusCodeValue());
        assertTrue(resp2.getBody().maintainers().contains("maint"));
    }

    @Test
    void maintainer_not_listed_cannot_update() {
        // Admin erstellt Region ohne maintainer
        HttpHeaders ha = bearer(adminToken);
        String body = "{\"name\":\"q4\",\"apiUrl\":\"http://api/q4\",\"publicSignKey\":\"k\",\"maintainers\":\"admin\"}";
        ResponseEntity<RegionControllerResponse> create = restTemplate.postForEntity("/universe/Region", new HttpEntity<>(body, ha), RegionControllerResponse.class);
        assertEquals(201, create.getStatusCodeValue());
        String id = create.getBody().id();
        // Maintainer versucht Update
        HttpHeaders hm = bearer(maintainerToken);
        String upd = "{\"name\":\"q4new\"}";
        ResponseEntity<RegionControllerResponse> update = restTemplate.exchange("/universe/Region/"+id, HttpMethod.PUT, new HttpEntity<>(upd, hm), RegionControllerResponse.class);
        assertEquals(403, update.getStatusCodeValue());
    }

    @Test
    void maintainer_listed_can_update() {
        HttpHeaders ha = bearer(adminToken);
        String body = "{\"name\":\"q5\",\"apiUrl\":\"http://api/q5\",\"publicSignKey\":\"k\",\"maintainers\":\"admin,maint\"}";
        ResponseEntity<RegionControllerResponse> create = restTemplate.postForEntity("/universe/Region", new HttpEntity<>(body, ha), RegionControllerResponse.class);
        String id = create.getBody().id();
        HttpHeaders hm = bearer(maintainerToken);
        String upd = "{\"name\":\"q5new\"}";
        ResponseEntity<RegionControllerResponse> update = restTemplate.exchange("/universe/Region/"+id, HttpMethod.PUT, new HttpEntity<>(upd, hm), RegionControllerResponse.class);
        assertEquals(200, update.getStatusCodeValue());
        assertEquals("q5new", update.getBody().name());
    }

    @Test
    void admin_can_delete() {
        HttpHeaders ha = bearer(adminToken);
        String body = "{\"name\":\"q6\",\"apiUrl\":\"http://api/q6\",\"publicSignKey\":\"k\",\"maintainers\":\"admin\"}";
        ResponseEntity<RegionControllerResponse> create = restTemplate.postForEntity("/universe/Region", new HttpEntity<>(body, ha), RegionControllerResponse.class);
        String id = create.getBody().id();
        ResponseEntity<Void> del = restTemplate.exchange("/universe/Region/"+id, HttpMethod.DELETE, new HttpEntity<>(ha), Void.class);
        assertEquals(204, del.getStatusCodeValue());
    }

    private HttpHeaders bearer(String token) { HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setBearerAuth(token); return h; }

    // Minimal DTO for responses
    public record RegionControllerResponse(String id, String name, String apiUrl, String publicSignKey, java.util.List<String> maintainers) {}
}

