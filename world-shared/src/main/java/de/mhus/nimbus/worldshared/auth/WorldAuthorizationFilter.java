package de.mhus.nimbus.worldshared.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that handles authentication for world services using shared secret.
 * Validates authentication headers and stores user context in request attributes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorldAuthorizationFilter extends OncePerRequestFilter {

    public static final String AUTH_CONTEXT_ATTRIBUTE = "worldAuthContext";

    private final WorldAuthorizationUtils authUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        // Extract headers
        String authHeader = request.getHeader(WorldAuthorizationUtils.AUTH_HEADER);
        String username = request.getHeader(WorldAuthorizationUtils.USERNAME_HEADER);
        String rolesHeader = request.getHeader(WorldAuthorizationUtils.ROLES_HEADER);

        WorldAuthContext authContext;

        // Validate authentication
        if (authUtils.validateAuthHeader(authHeader)) {
            // Extract and validate username
            String validatedUsername = authUtils.extractUsername(username);
            if (validatedUsername != null) {
                // Extract roles
                List<String> roles = authUtils.extractRoles(rolesHeader);

                // Create authenticated context
                authContext = authUtils.createAuthContext(validatedUsername, roles);
                log.debug("Authenticated user: {} with roles: {}", validatedUsername, roles);
            } else {
                log.debug("Invalid username in request");
                authContext = authUtils.createUnauthenticatedContext();
            }
        } else {
            log.debug("Authentication failed for request to: {}", request.getRequestURI());
            authContext = authUtils.createUnauthenticatedContext();
        }

        // Store context in request attributes
        request.setAttribute(AUTH_CONTEXT_ATTRIBUTE, authContext);

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filtering for health check endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.equals("/health") ||
               path.equals("/status");
    }
}
