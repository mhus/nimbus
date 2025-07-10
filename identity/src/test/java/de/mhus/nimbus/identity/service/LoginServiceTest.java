package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.LoginStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für LoginService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginServiceTest {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Test
    void testSuccessfulLogin() {
        // Given
        String username = "loginuser";
        String email = "login@example.com";
        String password = "password123";
        User createdUser = userService.createUser(username, email, password, "Login", "User");

        LoginRequest request = LoginRequest.newBuilder()
                .setRequestId("login-test-1")
                .setUsername(username)
                .setPassword(password)
                .setTimestamp(Instant.now())
                .setClientInfo("test-client")
                .build();

        // When
        LoginResponse response = loginService.processLoginRequest(request);

        // Then
        assertNotNull(response);
        assertEquals("login-test-1", response.getRequestId());
        assertEquals(LoginStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getToken());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getUser());
        assertEquals(createdUser.getId(), response.getUser().getId());
        assertEquals(username, response.getUser().getUsername());
        assertEquals(email, response.getUser().getEmail());
        assertNull(response.getErrorMessage());

        // Validiere JWT Token
        String token = response.getToken();
        assertFalse(jwtService.isTokenExpired(token));
        assertEquals(username, jwtService.extractUsername(token));
        assertEquals(createdUser.getId(), jwtService.extractUserId(token));
    }

    @Test
    void testLoginWithEmail() {
        // Given
        String username = "emailuser";
        String email = "email@example.com";
        String password = "password123";
        User createdUser = userService.createUser(username, email, password, "Email", "User");

        LoginRequest request = LoginRequest.newBuilder()
                .setRequestId("login-test-2")
                .setUsername(email) // Login mit E-Mail statt Username
                .setPassword(password)
                .setTimestamp(Instant.now())
                .build();

        // When
        LoginResponse response = loginService.processLoginRequest(request);

        // Then
        assertEquals(LoginStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getToken());
        assertEquals(createdUser.getId(), response.getUser().getId());
    }

    @Test
    void testLoginWithInvalidCredentials() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        userService.createUser(username, email, "correctpassword", "Test", "User");

        LoginRequest request = LoginRequest.newBuilder()
                .setRequestId("login-test-3")
                .setUsername(username)
                .setPassword("wrongpassword")
                .setTimestamp(Instant.now())
                .build();

        // When
        LoginResponse response = loginService.processLoginRequest(request);

        // Then
        assertEquals(LoginStatus.INVALID_CREDENTIALS, response.getStatus());
        assertNull(response.getToken());
        assertNull(response.getUser());
        assertEquals("Invalid credentials", response.getErrorMessage());
    }

    @Test
    void testLoginWithNonExistentUser() {
        // Given
        LoginRequest request = LoginRequest.newBuilder()
                .setRequestId("login-test-4")
                .setUsername("nonexistent")
                .setPassword("password")
                .setTimestamp(Instant.now())
                .build();

        // When
        LoginResponse response = loginService.processLoginRequest(request);

        // Then
        assertEquals(LoginStatus.USER_NOT_FOUND, response.getStatus());
        assertNull(response.getToken());
        assertNull(response.getUser());
        assertEquals("User not found", response.getErrorMessage());
    }

    @Test
    void testLoginWithInactiveUser() {
        // Given
        String username = "inactiveuser";
        String email = "inactive@example.com";
        User user = userService.createUser(username, email, "password123", "Inactive", "User");

        // Deaktiviere den User
        userService.deactivateUser(user.getId());

        LoginRequest request = LoginRequest.newBuilder()
                .setRequestId("login-test-5")
                .setUsername(username)
                .setPassword("password123")
                .setTimestamp(Instant.now())
                .build();

        // When
        LoginResponse response = loginService.processLoginRequest(request);

        // Then
        assertEquals(LoginStatus.USER_INACTIVE, response.getStatus());
        assertNull(response.getToken());
        assertNull(response.getUser());
        assertEquals("User account is inactive", response.getErrorMessage());
    }

    @Test
    void testValidateLoginRequest() {
        // Valid request
        LoginRequest validRequest = LoginRequest.newBuilder()
                .setRequestId("valid-request")
                .setUsername("testuser")
                .setPassword("password")
                .setTimestamp(Instant.now())
                .build();

        assertDoesNotThrow(() -> loginService.validateLoginRequest(validRequest));

        // Invalid request - missing username

        assertThrows(org.apache.avro.AvroMissingFieldException.class,
                    () -> {
                        LoginRequest invalidRequest = LoginRequest.newBuilder()
                                .setRequestId("invalid-request")
                                .setPassword("password")
                                .setTimestamp(Instant.now())
                                .build();
                        loginService.validateLoginRequest(invalidRequest);
                    });
    }
}
