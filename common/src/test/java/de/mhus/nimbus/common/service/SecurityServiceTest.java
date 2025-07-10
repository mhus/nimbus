package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.LoginStatus;
import de.mhus.nimbus.shared.avro.LoginUserInfo;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import de.mhus.nimbus.shared.avro.PublicKeyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test für SecurityService
 * Folgt Spring Boot Testing Conventions
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private IdentityClient identityClient;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(identityClient);
    }

    @Test
    void testSuccessfulLogin() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String username = "testuser";
        String password = "password123";
        LoginResponse mockResponse = createSuccessLoginResponse();

        when(identityClient.requestLogin(eq(username), eq(password), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        SecurityService.LoginResult result = securityService.login(username, password);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("jwt-token-123", result.getToken());
        assertNotNull(result.getExpiresAt());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("test@example.com", result.getUser().getEmail());

        verify(identityClient).requestLogin(eq(username), eq(password), anyString());
    }

    @Test
    void testLoginWithInvalidCredentials() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String username = "testuser";
        String password = "wrongpassword";
        LoginResponse mockResponse = createErrorLoginResponse(LoginStatus.INVALID_CREDENTIALS);

        when(identityClient.requestLogin(eq(username), eq(password), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> securityService.login(username, password));

        assertTrue(exception.getCause() instanceof NimbusException);
        NimbusException nimbusException = (NimbusException) exception.getCause();
        assertEquals("Invalid username or password", nimbusException.getMessage());
        assertEquals("INVALID_CREDENTIALS", nimbusException.getErrorCode());
    }

    @Test
    void testLoginWithUserNotFound() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String username = "nonexistent";
        String password = "password123";
        LoginResponse mockResponse = createErrorLoginResponse(LoginStatus.USER_NOT_FOUND);

        when(identityClient.requestLogin(eq(username), eq(password), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> securityService.login(username, password));

        assertTrue(exception.getCause() instanceof NimbusException);
        NimbusException nimbusException = (NimbusException) exception.getCause();
        assertEquals("User not found", nimbusException.getMessage());
        assertEquals("USER_NOT_FOUND", nimbusException.getErrorCode());
    }

    @Test
    void testLoginWithEmptyUsername() {
        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> securityService.login("", "password123"));

        assertTrue(exception.getCause() instanceof NimbusException);
        NimbusException nimbusException = (NimbusException) exception.getCause();
        assertEquals("Username cannot be null or empty", nimbusException.getMessage());
        assertEquals("VALIDATION_ERROR", nimbusException.getErrorCode());
    }

    @Test
    void testLoginWithNullPassword() {
        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> securityService.login("testuser", null));

        assertTrue(exception.getCause() instanceof NimbusException);
        NimbusException nimbusException = (NimbusException) exception.getCause();
        assertEquals("Password cannot be null or empty", nimbusException.getMessage());
        assertEquals("VALIDATION_ERROR", nimbusException.getErrorCode());
    }

    @Test
    void testLoginWithClientInfo() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String username = "testuser";
        String password = "password123";
        String clientInfo = "test-client";
        LoginResponse mockResponse = createSuccessLoginResponse();

        when(identityClient.requestLogin(eq(username), eq(password), eq(clientInfo)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        SecurityService.LoginResult result = securityService.login(username, password, clientInfo);

        // Then
        assertTrue(result.isSuccess());
        verify(identityClient).requestLogin(eq(username), eq(password), eq(clientInfo));
    }

    @Test
    void testGetPublicKeySuccess() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        when(identityClient.requestPublicKey())
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        SecurityService.PublicKeyInfo result = securityService.getPublicKey();

        // Then
        assertNotNull(result);
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...", result.getPublicKey());
        assertEquals("RSA", result.getKeyType());
        assertEquals("RS256", result.getAlgorithm());
        assertEquals("nimbus-identity-service", result.getIssuer());
        assertNotNull(result.getFetchedAt());

        verify(identityClient).requestPublicKey();
    }

    @Test
    void testGetPublicKeyCaching() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        when(identityClient.requestPublicKey())
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When - Erste Anfrage
        SecurityService.PublicKeyInfo result1 = securityService.getPublicKey();

        // When - Zweite Anfrage (sollte aus Cache kommen)
        SecurityService.PublicKeyInfo result2 = securityService.getPublicKey();

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getPublicKey(), result2.getPublicKey());
        assertTrue(securityService.hasValidPublicKey());

        // Verify nur ein Request gesendet wurde (wegen Caching)
        verify(identityClient, times(1)).requestPublicKey();
    }

    @Test
    void testGetPublicKeyForceRefresh() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        when(identityClient.requestPublicKey())
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Erste Anfrage
        SecurityService.PublicKeyInfo result1 = securityService.getPublicKey();

        // When - Force refresh
        SecurityService.PublicKeyInfo result2 = securityService.getPublicKey(true);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getPublicKey(), result2.getPublicKey());

        // Verify zwei Requests gesendet wurden
        verify(identityClient, times(2)).requestPublicKey();
    }

    @Test
    void testClearPublicKeyCache() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        PublicKeyResponse mockResponse = createSuccessPublicKeyResponse();

        when(identityClient.requestPublicKey())
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        securityService.getPublicKey();
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

        when(identityClient.requestPublicKey())
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> securityService.getPublicKey());

        assertTrue(exception.getCause() instanceof NimbusException);
        NimbusException nimbusException = (NimbusException) exception.getCause();
        assertEquals("Public key request failed", nimbusException.getMessage());
        assertEquals("PUBLIC_KEY_ERROR", nimbusException.getErrorCode());
    }

    @Test
    void testAsyncLogin() {
        // Given
        String username = "testuser";
        String password = "password123";
        String clientInfo = "test-client";
        LoginResponse mockResponse = createSuccessLoginResponse();

        when(identityClient.requestLogin(eq(username), eq(password), eq(clientInfo)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        CompletableFuture<SecurityService.LoginResult> resultFuture =
                securityService.loginAsync(username, password, clientInfo);

        // Then
        assertDoesNotThrow(() -> {
            SecurityService.LoginResult result = resultFuture.get();
            assertTrue(result.isSuccess());
            assertEquals("jwt-token-123", result.getToken());
        });
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
                .setExpiresAt(Instant.now().plusSeconds(3600))
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
