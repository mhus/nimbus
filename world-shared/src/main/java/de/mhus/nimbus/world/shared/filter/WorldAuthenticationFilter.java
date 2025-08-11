package de.mhus.nimbus.world.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorldAuthenticationFilter extends OncePerRequestFilter {

    @Value("${world.auth.secret}")
    private String authSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("X-World-Auth");

        if (authHeader != null && authHeader.equals(authSecret)) {
            // Create authentication token with world service roles
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "world-service",
                null,
                List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_CREATOR"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("World service authenticated successfully");
        } else {
            log.warn("Invalid or missing world authentication header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip authentication for actuator endpoints
        return path.startsWith("/actuator");
    }
}
