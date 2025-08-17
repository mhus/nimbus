package de.mhus.nimbus.worldgenerator.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class SharedSecretAuthenticationFilter extends OncePerRequestFilter {

    private final String sharedSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {

        String headerSecret = request.getHeader("X-Shared-Secret");

        if (headerSecret == null || !headerSecret.equals(sharedSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            response.setContentType("application/json");
            return;
        }

        // Authentifizierung erfolgreich
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "generator-service", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
