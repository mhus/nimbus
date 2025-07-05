package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.common.util.RequestIdUtils;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.LoginStatus;
import de.mhus.nimbus.shared.avro.LoginUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test für SecurityService
 * Folgt Spring Boot Testing Conventions
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RequestIdUtils requestIdUtils;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(kafkaTemplate, requestIdUtils);
        when(requestIdUtils.generateRequestId(anyString())).thenReturn("test-request-123");
    }

    @Test
    void testSuccessfulLogin() {
        // Given
        String username = "testuser";
        String password = "password123";

        // Simuliere Login-Response
        LoginResponse mockResponse = createSuccessLoginResponse();

        // Simuliere asynchrone Response
        new Thread(() -> {
            try {
                Thread.sleep(100); // Kurze Verzögerung
                securityService.handleLoginResponse(mockResponse, "login-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When
        SecurityService.LoginResult result = securityService.login(username, password);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("jwt-token-123", result.getToken());
        assertNotNull(result.getExpiresAt());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("test@example.com", result.getUser().getEmail());

        // Verify Kafka interaction
        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(kafkaTemplate).send(eq("login-request"), eq("test-request-123"), requestCaptor.capture());

        LoginRequest sentRequest = requestCaptor.getValue();
        assertEquals(username, sentRequest.getUsername());
        assertEquals(password, sentRequest.getPassword());
        assertEquals("test-request-123", sentRequest.getRequestId());
    }

    @Test
    void testLoginWithInvalidCredentials() {
        // Given
        String username = "testuser";
        String password = "wrongpassword";

        LoginResponse mockResponse = createErrorLoginResponse(LoginStatus.INVALID_CREDENTIALS);

        // Simuliere asynchrone Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handleLoginResponse(mockResponse, "login-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When & Then
        NimbusException exception = assertThrows(NimbusException.class, () -> {
            securityService.login(username, password);
        });

        assertEquals("Invalid username or password", exception.getMessage());
        assertEquals("INVALID_CREDENTIALS", exception.getErrorCode());
    }

    @Test
    void testLoginWithUserNotFound() {
        // Given
        String username = "nonexistent";
        String password = "password123";

        LoginResponse mockResponse = createErrorLoginResponse(LoginStatus.USER_NOT_FOUND);

        // Simuliere asynchrone Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handleLoginResponse(mockResponse, "login-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When & Then
        NimbusException exception = assertThrows(NimbusException.class, () -> {
            securityService.login(username, password);
        });

        assertEquals("User not found", exception.getMessage());
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void testLoginWithEmptyUsername() {
        // When & Then
        NimbusException exception = assertThrows(NimbusException.class, () -> {
            securityService.login("", "password123");
        });

        assertEquals("Username cannot be null or empty", exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
    }

    @Test
    void testLoginWithNullPassword() {
        // When & Then
        NimbusException exception = assertThrows(NimbusException.class, () -> {
            securityService.login("testuser", null);
        });

        assertEquals("Password cannot be null or empty", exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
    }

    @Test
    void testLoginWithClientInfo() {
        // Given
        String username = "testuser";
        String password = "password123";
        String clientInfo = "test-client";

        LoginResponse mockResponse = createSuccessLoginResponse();

        // Simuliere asynchrone Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handleLoginResponse(mockResponse, "login-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When
        SecurityService.LoginResult result = securityService.login(username, password, clientInfo);

        // Then
        assertTrue(result.isSuccess());

        // Verify client info was sent
        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(kafkaTemplate).send(anyString(), anyString(), requestCaptor.capture());

        LoginRequest sentRequest = requestCaptor.getValue();
        assertEquals(clientInfo, sentRequest.getClientInfo());
    }

    private LoginResponse createSuccessLoginResponse() {
        LoginUserInfo userInfo = LoginUserInfo.newBuilder()
                .setId(1L)
                .setUsername("testuser")
                .setEmail("test@example.com")
                .setFirstName("Test")
                .setLastName("User")
                .build();

        return LoginResponse.newBuilder()
                .setRequestId("test-request-123")
                .setStatus(LoginStatus.SUCCESS)
                .setToken("jwt-token-123")
                .setExpiresAt(Instant.now().plusSeconds(3600).toEpochMilli())
                .setUser(userInfo)
                .setTimestamp(Instant.now().toEpochMilli())
                .setErrorMessage(null)
                .build();
    }

    private LoginResponse createErrorLoginResponse(LoginStatus status) {
        return LoginResponse.newBuilder()
                .setRequestId("test-request-123")
                .setStatus(status)
                .setToken(null)
                .setExpiresAt(null)
                .setUser(null)
                .setTimestamp(Instant.now().toEpochMilli())
                .setErrorMessage("Login failed")
                .build();
    }
}
