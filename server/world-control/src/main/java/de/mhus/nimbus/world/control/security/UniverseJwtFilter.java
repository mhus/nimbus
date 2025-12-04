package de.mhus.nimbus.world.control.security;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Filter validiert Universe JWT und setzt userId als Request-Attribut "currentUserId".
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UniverseJwtFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "currentUserId";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            if (!token.isEmpty()) {
                Optional<io.jsonwebtoken.Jws<Claims>> jws = jwtService.validateTokenWithPublicKey(token, KeyType.UNIVERSE, KeyIntent.of("universe", KeyIntent.MAIN_JWT_TOKEN));
                if (jws.isPresent()) {
                    String subject = jws.get().getPayload().getSubject();
                    if (subject != null && !subject.isBlank()) {
                        request.setAttribute(ATTR_USER_ID, subject);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
