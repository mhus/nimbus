package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security Filter für alle Pfade unter /shared/**.
 * Erwartet ein Bearer JWT im Header Authorization. Verifiziert es gegen
 * den Universe MAIN_JWT_TOKEN Public Key Intent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SharedJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASE_PREFIX = "/shared"; // prüft alle Unterpfade

    private final JwtService jwtService;
    private final KeyService keyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!applies(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        var publicKeys = keyService.getPublicKeysForIntent(KeyType.UNIVERSE, KeyIntent.of("theUniverse", KeyIntent.MAIN_JWT_TOKEN));
        if (publicKeys.isEmpty()) {
            log.error("Kein Universe Public Key (Intent main-jwt-token, Owner 'theUniverse') verfügbar – lasse Zugriff auf {} zu", path);
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = auth.substring(7).trim();
        Jws<Claims> jws = jwtService.validateTokenWithPublicKey(token, KeyType.UNIVERSE, KeyIntent.of("theUniverse", KeyIntent.MAIN_JWT_TOKEN)).orElse(null);
        if (jws == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean applies(String path) {
        return path != null && path.startsWith(BASE_PREFIX);
    }
}
