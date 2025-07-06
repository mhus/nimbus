package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.common.util.RequestIdUtils;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.LoginStatus;
import de.mhus.nimbus.shared.avro.LoginUserInfo;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import de.mhus.nimbus.shared.avro.PublicKeyStatus;
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

    @Test
    void testGetPublicKeySuccess() {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        // Simuliere asynchrone Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handlePublicKeyResponse(mockResponse, "public-key-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When
        SecurityService.PublicKeyInfo result = securityService.getPublicKey();

        // Then
        assertNotNull(result);
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...", result.getPublicKey());
        assertEquals("RSA", result.getKeyType());
        assertEquals("RS256", result.getAlgorithm());
        assertEquals("nimbus-identity-service", result.getIssuer());
        assertNotNull(result.getFetchedAt());

        // Verify Kafka interaction
        verify(kafkaTemplate, atLeastOnce()).send(eq("public-key-request"), anyString(), any(PublicKeyRequest.class));
    }

    @Test
    void testGetPublicKeyCaching() {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        // Simuliere erste Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handlePublicKeyResponse(mockResponse, "public-key-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When - Erste Anfrage
        SecurityService.PublicKeyInfo result1 = securityService.getPublicKey();

        // When - Zweite Anfrage (sollte aus Cache kommen)
        SecurityService.PublicKeyInfo result2 = securityService.getPublicKey();

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getPublicKey(), result2.getPublicKey());
        assertTrue(securityService.hasValidPublicKey());

        // Verify nur ein Kafka-Request gesendet wurde
        verify(kafkaTemplate, times(1)).send(eq("public-key-request"), anyString(), any(PublicKeyRequest.class));
    }

    @Test
    void testGetPublicKeyForceRefresh() {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        // Simuliere erste Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handlePublicKeyResponse(mockResponse, "public-key-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Erste Anfrage
        SecurityService.PublicKeyInfo result1 = securityService.getPublicKey();

        // Simuliere zweite Response für forced refresh
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handlePublicKeyResponse(mockResponse, "public-key-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When - Force refresh
        SecurityService.PublicKeyInfo result2 = securityService.getPublicKey(true);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getPublicKey(), result2.getPublicKey());

        // Verify zwei Kafka-Requests gesendet wurden
        verify(kafkaTemplate, times(2)).send(eq("public-key-request"), anyString(), any(PublicKeyRequest.class));
    }

    @Test
    void testClearPublicKeyCache() {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        // Simuliere Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handlePublicKeyResponse(mockResponse, "public-key-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When
        SecurityService.PublicKeyInfo result = securityService.getPublicKey();
        assertTrue(securityService.hasValidPublicKey());

        securityService.clearPublicKeyCache();

        // Then
        assertFalse(securityService.hasValidPublicKey());
        assertNull(securityService.getCachedPublicKey());
    }

    @Test
    void testGetPublicKeyError() {
        // Given
        PublicKeyResponse mockResponse = createErrorPublicKeyResponse();

        // Simuliere Error Response
        new Thread(() -> {
            try {
                Thread.sleep(100);
                securityService.handlePublicKeyResponse(mockResponse, "public-key-response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // When & Then
        NimbusException exception = assertThrows(NimbusException.class, () -> {
            securityService.getPublicKey();
        });

        assertEquals("Public key request failed", exception.getMessage());
        assertEquals("PUBLIC_KEY_ERROR", exception.getErrorCode());
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
                .setTimestamp(Instant.now())
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
                .setTimestamp(Instant.now())
                .setErrorMessage("Login failed")
                .build();
    }

    private PublicKeyResponse createSuccessPublicKeyResponse() {
        return PublicKeyResponse.newBuilder()
                .setRequestId("test-request-123")
                .setStatus(PublicKeyStatus.SUCCESS)
                .setPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...")
                .setKeyType("RSA")
                .setAlgorithm("RS256")
                .setIssuer("nimbus-identity-service")
                .setTimestamp(Instant.now())
                .setErrorMessage(null)
                .build();
    }

    private PublicKeyResponse createErrorPublicKeyResponse() {
        return PublicKeyResponse.newBuilder()
                .setRequestId("test-request-123")
                .setStatus(PublicKeyStatus.ERROR)
                .setPublicKey(null)
                .setKeyType(null)
                .setAlgorithm(null)
                .setIssuer(null)
                .setTimestamp(Instant.now())
                .setErrorMessage("Public key request failed")
                .build();
    }
}
