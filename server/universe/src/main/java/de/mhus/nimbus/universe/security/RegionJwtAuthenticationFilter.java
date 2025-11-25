package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyKind;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.universe.UniverseProperties;
import de.mhus.nimbus.universe.region.URegionService;
import de.mhus.nimbus.shared.security.SharedClientService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Filter, der für alle Aufrufe unter /universe/region/{regionId}/** ein Bearer-Token
 * gegen den Regions-Key prüft. Owner des KeyIds ist die regionId aus dem Pfad; die UUID
 * wird aus JwtProperties.regionKeyUuid bezogen (Fallback: Suffix von JwtProperties.keyId nach ':').
 */
 @Component
 @RequiredArgsConstructor
public class RegionJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASE_PREFIX1 = "/universe/region";
    private static final String BASE_PREFIX2 = BASE_PREFIX1 + "/";

    private final JwtService jwtService;
    private final UniverseProperties jwtProperties;
    private final URegionService regionService;
    private final KeyService keyService;

    @Value("${region.server.id:local-region-server}")
    private String regionServerId;

    @Value("${universe.base.url:http://localhost:8080}")
    private String universeBaseUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!applies(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var token = auth.substring(7).trim();

        // validate token from region server key
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.equals("/universe/region")) {
            try {
                var claims = jwtService.validateTokenWithPublicKey(token, KeyType.REGION, KeyIntent.of(regionServerId, KeyIntent.REGION_SERVER_JWT_TOKEN));
                if (claims.isPresent()) {
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception ignore) {}
        }

        String regionId = extractRegionId(path);
        if (regionId == null || regionId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // get region from database
        var region = regionService.getById(regionId);
        if (!region.isPresent() || !region.get().isEnabled()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // validate token
        var claims = jwtService.validateTokenWithPublicKey(token, KeyType.REGION, KeyIntent.of(regionId, KeyIntent.REGION_JWT_TOKEN));
        if (claims.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean applies(String path) {
        return path != null &&
                (path.equals(BASE_PREFIX1) || path.startsWith(BASE_PREFIX2));
    }

    private String extractRegionId(String path) {
        // erwartet Pfad wie: /universe/region/{regionId}/...
        if (path == null || !path.startsWith(BASE_PREFIX2)) return null;
        String rest = path.substring(BASE_PREFIX2.length());
        int slash = rest.indexOf('/');
        if (slash < 0) return rest; // nur regionId vorhanden
        return rest.substring(0, slash);
    }


}
