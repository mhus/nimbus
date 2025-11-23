package de.mhus.nimbus.universe.favorit;

import de.mhus.nimbus.universe.auth.ULoginResponse;
import de.mhus.nimbus.universe.UniverseProperties;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FavoritControllerIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @Autowired
    private UUserService userService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UniverseProperties jwtProperties;

    private String token;

    @BeforeEach
    void setup() {
        UUser u = userService.createUser("alpha","alpha@example.com");
        userService.setPassword(u.getId(), "secret123");
        token = login("alpha", "secret123");
    }

    private String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        ResponseEntity<ULoginResponse> resp = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), ULoginResponse.class);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        return resp.getBody().token();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    void unauthorized_access_list() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/favorits", String.class);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void full_crud_flow() {
        // Create
        String createJson = "{\"regionId\":\"q1\",\"solarSystemId\":\"s1\",\"worldId\":\"w1\",\"entryPointId\":\"e1\",\"title\":\"Title One\",\"favorit\":true}";
        ResponseEntity<Map> createResp = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(createJson, authHeaders()), Map.class);
        assertEquals(201, createResp.getStatusCode().value());
        String id = (String) createResp.getBody().get("id");
        assertNotNull(id);
        assertEquals("q1", createResp.getBody().get("regionId"));
        assertTrue((Boolean) createResp.getBody().get("favorit"));

        // List favorites
        ResponseEntity<Map[]> favList = restTemplate.exchange("/api/favorits", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(200, favList.getStatusCode().value());
        assertEquals(1, favList.getBody().length);

        // Get single
        ResponseEntity<Map> getResp = restTemplate.exchange("/api/favorits/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        assertEquals(200, getResp.getStatusCode().value());
        assertEquals(id, getResp.getBody().get("id"));

        // Update (change title and favorit flag false)
        String updateJson = "{\"regionId\":\"q1\",\"solarSystemId\":\"s1\",\"worldId\":\"w1\",\"entryPointId\":\"e1\",\"title\":\"Title Updated\",\"favorit\":false}";
        ResponseEntity<Map> updateResp = restTemplate.exchange("/api/favorits/" + id, HttpMethod.PUT, new HttpEntity<>(updateJson, authHeaders()), Map.class);
        assertEquals(200, updateResp.getStatusCode().value());
        assertEquals("Title Updated", updateResp.getBody().get("title"));
        assertFalse((Boolean) updateResp.getBody().get("favorit"));

        // Toggle back to true
        ResponseEntity<Map> toggleResp = restTemplate.postForEntity("/api/favorits/" + id + "/toggle?favorit=true", new HttpEntity<>(authHeaders()), Map.class);
        assertEquals(200, toggleResp.getStatusCode().value());
        assertTrue((Boolean) toggleResp.getBody().get("favorit"));

        // List favorites again should show still one favorite
        favList = restTemplate.exchange("/api/favorits", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(200, favList.getStatusCode().value());
        assertEquals(1, favList.getBody().length);

        // Delete
        ResponseEntity<Void> deleteResp = restTemplate.exchange("/api/favorits/" + id, HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Void.class);
        assertEquals(204, deleteResp.getStatusCode().value());

        // Get after delete -> 404
        ResponseEntity<Map> getAfterDelete = restTemplate.exchange("/api/favorits/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        assertEquals(404, getAfterDelete.getStatusCode().value());
    }

    @Test
    void create_invalid_missing_regionId_400() {
        String createJson = "{\"solarSystemId\":\"s1\",\"worldId\":\"w1\",\"entryPointId\":\"e1\",\"title\":\"Title\",\"favorit\":true}"; // missing regionId
        ResponseEntity<String> resp = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(createJson, authHeaders()), String.class);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void unauthorized_create_401() {
        String createJson = "{\"regionId\":\"qX\",\"title\":\"NoAuth\",\"favorit\":true}";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(createJson, h), String.class);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void update_nonOwner_404() {
        // Create favorite with second user
        UUser b = userService.createUser("beta","beta@example.com");
        userService.setPassword(b.getId(), "secret123");
        String betaToken = login("beta","secret123");
        HttpHeaders betaHeaders = new HttpHeaders();
        betaHeaders.setBearerAuth(betaToken);
        betaHeaders.setContentType(MediaType.APPLICATION_JSON);
        String createJson = "{\"regionId\":\"qBeta\",\"title\":\"Beta Fav\",\"favorit\":true}";
        ResponseEntity<Map> betaCreate = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(createJson, betaHeaders), Map.class);
        assertEquals(201, betaCreate.getStatusCode().value());
        String favId = (String) betaCreate.getBody().get("id");
        // Attempt update with alpha's token (should 404)
        String updateJson = "{\"regionId\":\"qBeta\",\"title\":\"Changed\",\"favorit\":false}";
        ResponseEntity<Map> updateResp = restTemplate.exchange("/api/favorits/"+favId, HttpMethod.PUT, new HttpEntity<>(updateJson, authHeaders()), Map.class);
        assertEquals(404, updateResp.getStatusCode().value());
    }

    @Test
    void toggle_to_false_removes_from_favorites_list() {
        // Create favorite
        String createJson = "{\"regionId\":\"qToggle\",\"title\":\"Toggle Favorite\",\"favorit\":true}";
        ResponseEntity<Map> createResp = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(createJson, authHeaders()), Map.class);
        assertEquals(201, createResp.getStatusCode().value());
        String id = (String) createResp.getBody().get("id");
        // Ensure favorites list has one
        ResponseEntity<Map[]> favList = restTemplate.exchange("/api/favorits", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(1, favList.getBody().length);
        // Toggle off
        ResponseEntity<Map> toggleResp = restTemplate.postForEntity("/api/favorits/"+id+"/toggle?favorit=false", new HttpEntity<>(authHeaders()), Map.class);
        assertEquals(200, toggleResp.getStatusCode().value());
        assertFalse((Boolean) toggleResp.getBody().get("favorit"));
        // Favorites list now empty
        favList = restTemplate.exchange("/api/favorits", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(0, favList.getBody().length);
        // All list still has entry
        ResponseEntity<Map[]> allList = restTemplate.exchange("/api/favorits/all", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(1, allList.getBody().length);
    }

    @Test
    void create_blank_title_400() {
        String json = "{\"regionId\":\"qBlank\",\"title\":\"\",\"favorit\":true}";
        ResponseEntity<String> resp = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(json, authHeaders()), String.class);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void create_blank_regionId_400() {
        String json = "{\"regionId\":\"\",\"title\":\"TT\",\"favorit\":true}";
        ResponseEntity<String> resp = restTemplate.postForEntity("/api/favorits", new HttpEntity<>(json, authHeaders()), String.class);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void delete_nonExisting_404() {
        ResponseEntity<Void> resp = restTemplate.exchange("/api/favorits/nonexist", HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Void.class);
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void toggle_nonExisting_404() {
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/favorits/nonexist/toggle?favorit=true", new HttpEntity<>(authHeaders()), Map.class);
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void listAll_multiple_entries_mixed_favorit() {
        // Create favorite true
        String json1 = "{\"regionId\":\"qA\",\"title\":\"A\",\"favorit\":true}";
        restTemplate.postForEntity("/api/favorits", new HttpEntity<>(json1, authHeaders()), Map.class);
        // Create favorite false
        String json2 = "{\"regionId\":\"qB\",\"title\":\"B\",\"favorit\":false}";
        restTemplate.postForEntity("/api/favorits", new HttpEntity<>(json2, authHeaders()), Map.class);
        // Favorites list should have only 1
        ResponseEntity<Map[]> favList = restTemplate.exchange("/api/favorits", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(1, favList.getBody().length);
        // All list should have 2
        ResponseEntity<Map[]> allList = restTemplate.exchange("/api/favorits/all", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map[].class);
        assertEquals(2, allList.getBody().length);
    }
}
