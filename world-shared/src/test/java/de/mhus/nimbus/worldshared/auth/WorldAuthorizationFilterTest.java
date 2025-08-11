package de.mhus.nimbus.worldshared.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldAuthorizationFilterTest {

    @Mock
    private WorldAuthorizationUtils authUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private WorldAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new WorldAuthorizationFilter(authUtils);
    }

    @Test
    void testDoFilterInternal_ValidAuthentication() throws ServletException, IOException {
        // Arrange
        String authHeader = "valid-hash";
        String username = "testuser";
        String rolesHeader = "ADMIN,USER";
        List<String> roles = List.of("ADMIN", "USER");

        when(request.getHeader(WorldAuthorizationUtils.AUTH_HEADER)).thenReturn(authHeader);
        when(request.getHeader(WorldAuthorizationUtils.USERNAME_HEADER)).thenReturn(username);
        when(request.getHeader(WorldAuthorizationUtils.ROLES_HEADER)).thenReturn(rolesHeader);

        when(authUtils.validateAuthHeader(authHeader)).thenReturn(true);
        when(authUtils.extractUsername(username)).thenReturn(username);
        when(authUtils.extractRoles(rolesHeader)).thenReturn(roles);

        WorldAuthContext expectedContext = WorldAuthContext.builder()
            .username(username)
            .roles(roles)
            .authenticated(true)
            .build();
        when(authUtils.createAuthContext(username, roles)).thenReturn(expectedContext);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request).setAttribute(eq(WorldAuthorizationFilter.AUTH_CONTEXT_ATTRIBUTE), any(WorldAuthContext.class));
        verify(filterChain).doFilter(request, response);
        verify(authUtils).validateAuthHeader(authHeader);
        verify(authUtils).extractUsername(username);
        verify(authUtils).extractRoles(rolesHeader);
        verify(authUtils).createAuthContext(username, roles);
    }

    @Test
    void testDoFilterInternal_InvalidAuthentication() throws ServletException, IOException {
        // Arrange
        String authHeader = "invalid-hash";
        when(request.getHeader(WorldAuthorizationUtils.AUTH_HEADER)).thenReturn(authHeader);
        when(authUtils.validateAuthHeader(authHeader)).thenReturn(false);

        WorldAuthContext unauthenticatedContext = WorldAuthContext.builder()
            .authenticated(false)
            .build();
        when(authUtils.createUnauthenticatedContext()).thenReturn(unauthenticatedContext);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request).setAttribute(eq(WorldAuthorizationFilter.AUTH_CONTEXT_ATTRIBUTE), any(WorldAuthContext.class));
        verify(filterChain).doFilter(request, response);
        verify(authUtils).validateAuthHeader(authHeader);
        verify(authUtils).createUnauthenticatedContext();
        verify(authUtils, never()).extractUsername(any());
        verify(authUtils, never()).extractRoles(any());
    }

    @Test
    void testDoFilterInternal_ValidAuthButInvalidUsername() throws ServletException, IOException {
        // Arrange
        String authHeader = "valid-hash";
        String invalidUsername = "invalid@user";
        when(request.getHeader(WorldAuthorizationUtils.AUTH_HEADER)).thenReturn(authHeader);
        when(request.getHeader(WorldAuthorizationUtils.USERNAME_HEADER)).thenReturn(invalidUsername);

        when(authUtils.validateAuthHeader(authHeader)).thenReturn(true);
        when(authUtils.extractUsername(invalidUsername)).thenReturn(null);

        WorldAuthContext unauthenticatedContext = WorldAuthContext.builder()
            .authenticated(false)
            .build();
        when(authUtils.createUnauthenticatedContext()).thenReturn(unauthenticatedContext);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request).setAttribute(eq(WorldAuthorizationFilter.AUTH_CONTEXT_ATTRIBUTE), any(WorldAuthContext.class));
        verify(filterChain).doFilter(request, response);
        verify(authUtils).validateAuthHeader(authHeader);
        verify(authUtils).extractUsername(invalidUsername);
        verify(authUtils).createUnauthenticatedContext();
        verify(authUtils, never()).extractRoles(any());
        verify(authUtils, never()).createAuthContext(any(), any());
    }

    @Test
    void testShouldNotFilter_HealthEndpoints() {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/health");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/status");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/actuator/metrics");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void testShouldNotFilter_ApiEndpoints() {
        when(request.getRequestURI()).thenReturn("/api/test");
        assertFalse(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/api/users");
        assertFalse(filter.shouldNotFilter(request));
    }
}
