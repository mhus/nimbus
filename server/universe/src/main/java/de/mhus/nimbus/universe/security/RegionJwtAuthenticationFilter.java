package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter, der für alle Aufrufe unter /universe/region/{regionId}/** ein Bearer-Token
 * gegen den Regions-Key prüft. Owner des KeyIds ist die regionId aus dem Pfad; die UUID
 * wird aus JwtProperties.regionKeyUuid bezogen (Fallback: Suffix von JwtProperties.keyId nach ':').
 */
// @Component
public class RegionJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASE_PREFIX = "/universe/region/";

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public RegionJwtAuthenticationFilter(JwtService jwtService, JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!applies(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String regionId = extractRegionId(path);
        if (regionId == null || regionId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = auth.substring(7).trim();

        String uuid = resolveRegionKeyUuid();
        if (uuid == null || uuid.isBlank()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String keyId = regionId + ":" + uuid;

        Optional<Jws<Claims>> claims = jwtService.validateTokenWithSecretKey(token, keyId);
        if (claims.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean applies(String path) {
        return path != null && path.startsWith(BASE_PREFIX);
    }

    private String extractRegionId(String path) {
        // erwartet Pfad wie: /universe/region/{regionId}/...
        if (path == null) return null;
        String rest = path.substring(BASE_PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash < 0) return rest; // nur regionId vorhanden
        return rest.substring(0, slash);
    }

    private String resolveRegionKeyUuid() {
        String uuid = jwtProperties.getRegionKeyUuid();
        if (uuid != null && !uuid.isBlank()) return uuid.trim();
        String keyId = jwtProperties.getKeyId();
        if (keyId == null) return null;
        int idx = keyId.indexOf(':');
        if (idx < 0 || idx >= keyId.length() - 1) return null;
        return keyId.substring(idx + 1).trim();
    }
}
