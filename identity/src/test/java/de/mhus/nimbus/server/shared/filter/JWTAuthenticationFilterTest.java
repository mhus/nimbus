package de.mhus.nimbus.server.shared.filter;

import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JWTAuthenticationFilter.
 * Tests JWT token validation and security context setup.
 */
@ExtendWith(MockitoExtension.class)
public class JWTAuthenticationFilterTest {

    @Mock
    private IdentityServiceUtils identityServiceUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter responseWriter;

    @InjectMocks
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws IOException {
        SecurityContextHolder.clearContext();
        when(response.getWriter()).thenReturn(responseWriter);
    }

    @Test
    void doFilterInternal_ValidToken_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String userId = "testuser";
        List<String> roles = Arrays.asList("USER", "ADMIN");

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(validToken)).thenReturn(userId);
        when(identityServiceUtils.extractRoles(validToken)).thenReturn(roles);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("userRoles", roles);
        verify(filterChain).doFilter(request, response);

        // Verify Spring Security context is set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userId, authentication.getPrincipal());
        assertEquals(2, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void doFilterInternal_InvalidToken_Unauthorized() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        String authHeader = "Bearer " + invalidToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(invalidToken)).thenReturn(null);
        when(identityServiceUtils.extractRoles(invalidToken)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(responseWriter).write("Invalid JWT token");
        verify(filterChain, never()).doFilter(any(), any());
        verify(request, never()).setAttribute(eq("userId"), any());
        verify(request, never()).setAttribute(eq("userRoles"), any());

        // Verify Spring Security context is not set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
    }

    @Test
    void doFilterInternal_NoAuthHeader_ContinuesFilter() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(identityServiceUtils, never()).extractUserId(any());
        verify(identityServiceUtils, never()).extractRoles(any());
        verify(request, never()).setAttribute(eq("userId"), any());
        verify(request, never()).setAttribute(eq("userRoles"), any());

        // Verify Spring Security context is not set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
    }

    @Test
    void doFilterInternal_InvalidAuthHeaderFormat_ContinuesFilter() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(identityServiceUtils, never()).extractUserId(any());
        verify(identityServiceUtils, never()).extractRoles(any());
        verify(request, never()).setAttribute(eq("userId"), any());
        verify(request, never()).setAttribute(eq("userRoles"), any());
    }

    @Test
    void doFilterInternal_EmptyBearerToken_ContinuesFilter() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(identityServiceUtils, never()).extractUserId(any());
        verify(identityServiceUtils, never()).extractRoles(any());
        verify(request, never()).setAttribute(eq("userId"), any());
        verify(request, never()).setAttribute(eq("userRoles"), any());
    }

    @Test
    void doFilterInternal_ValidUserIdButNullRoles_Unauthorized() throws ServletException, IOException {
        // Given
        String validToken = "token.with.no.roles";
        String authHeader = "Bearer " + validToken;
        String userId = "testuser";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(validToken)).thenReturn(userId);
        when(identityServiceUtils.extractRoles(validToken)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(responseWriter).write("Invalid JWT token");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_NullUserIdButValidRoles_Unauthorized() throws ServletException, IOException {
        // Given
        String validToken = "token.with.no.userid";
        String authHeader = "Bearer " + validToken;
        List<String> roles = Arrays.asList("USER");

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(validToken)).thenReturn(null);
        when(identityServiceUtils.extractRoles(validToken)).thenReturn(roles);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(responseWriter).write("Invalid JWT token");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_EmptyRolesList_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String userId = "testuser";
        List<String> emptyRoles = Arrays.asList();

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(validToken)).thenReturn(userId);
        when(identityServiceUtils.extractRoles(validToken)).thenReturn(emptyRoles);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("userRoles", emptyRoles);
        verify(filterChain).doFilter(request, response);

        // Verify Spring Security context is set with empty authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userId, authentication.getPrincipal());
        assertEquals(0, authentication.getAuthorities().size());
    }

    @Test
    void doFilterInternal_SingleRole_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String userId = "testuser";
        List<String> singleRole = Arrays.asList("USER");

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(validToken)).thenReturn(userId);
        when(identityServiceUtils.extractRoles(validToken)).thenReturn(singleRole);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("userRoles", singleRole);
        verify(filterChain).doFilter(request, response);

        // Verify Spring Security context is set with correct authority
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userId, authentication.getPrincipal());
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void shouldNotFilter_LoginEndpoint_ReturnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/login");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_HealthEndpoint_ReturnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/health");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_PublicEndpoint_ReturnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/public/info");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ProtectedEndpoint_ReturnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/users/123");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldNotFilter_RootEndpoint_ReturnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(result);
    }

    @Test
    void doFilterInternal_RolesCaseInsensitive_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String userId = "testuser";
        List<String> mixedCaseRoles = Arrays.asList("user", "Admin", "MODERATOR");

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(identityServiceUtils.extractUserId(validToken)).thenReturn(userId);
        when(identityServiceUtils.extractRoles(validToken)).thenReturn(mixedCaseRoles);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("userRoles", mixedCaseRoles);
        verify(filterChain).doFilter(request, response);

        // Verify Spring Security context is set with correct authorities (uppercased)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(3, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MODERATOR")));
    }
}
