package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.user.UserService;
import de.mhus.nimbus.universe.user.User;
import de.mhus.nimbus.universe.security.JwtProperties;
import de.mhus.nimbus.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class LoginControllerIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void cleanAndSetup() {
        // create user and set password each test
        User u = userService.createUser("alpha","alpha@example.com");
        userService.setPassword(u.getId(), "secret123");
    }

    @Test
    void login_ok_returns_jwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"alpha\",\"password\":\"secret123\"}";
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), LoginResponse.class);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        String token = response.getBody().token();
        assertNotNull(token);
        Optional<Jws<Claims>> claimsOpt = jwtService.validateTokenWithSecretKey(token, jwtProperties.getKeyId());
        assertTrue(claimsOpt.isPresent());
        Claims claims = claimsOpt.get().getPayload();
        assertEquals(response.getBody().userId(), claims.getSubject());
        assertEquals("alpha", claims.get("username"));
        assertTrue(claims.getExpiration().toInstant().isAfter(Instant.now().plus(30, ChronoUnit.MINUTES)));
    }

    @Test
    void login_wrong_password_401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"alpha\",\"password\":\"bad\"}";
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), LoginResponse.class);
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void login_unknown_user_401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"ghost\",\"password\":\"secret123\"}";
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), LoginResponse.class);
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void login_bad_request_400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":null,\"password\":\"secret123\"}"; // invalid JSON value for username
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), LoginResponse.class);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void me_unauthorized_401() {
        ResponseEntity<MeResponse> resp = restTemplate.getForEntity("/api/me", MeResponse.class);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void me_authorized_ok() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"alpha\",\"password\":\"secret123\"}";
        ResponseEntity<LoginResponse> login = restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), LoginResponse.class);
        assertEquals(200, login.getStatusCode().value());
        String token = login.getBody().token();
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        ResponseEntity<MeResponse> me = restTemplate.exchange("/api/me", HttpMethod.GET, new HttpEntity<>(authHeaders), MeResponse.class);
        assertEquals(200, me.getStatusCode().value());
        assertEquals(login.getBody().userId(), me.getBody().userId());
        assertEquals("alpha", me.getBody().username());
    }

    @Test
    void logout_no_token_ok() {
        ResponseEntity<Void> resp = restTemplate.getForEntity("/api/auth/logout", Void.class);
        assertEquals(200, resp.getStatusCode().value());
    }
}
