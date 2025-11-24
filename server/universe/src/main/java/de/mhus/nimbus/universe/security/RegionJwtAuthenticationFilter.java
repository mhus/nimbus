package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.KeyIntent;
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

    private static final String BASE_PREFIX = "/universe/region/";

    private final JwtService jwtService;
    private final UniverseProperties jwtProperties;
    private final URegionService regionService;
    private final KeyService keyService;
    private final SharedClientService sharedClientService;

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

        if ("POST".equalsIgnoreCase(request.getMethod()) && path.matches("/universe/region/region/[^/]+/?")) {
            try {
                boolean exists = sharedClientService.existsKey(universeBaseUrl, "REGION", "PUBLIC", regionServerId, KeyIntent.MAIN_JWT_TOKEN);
                if (!exists) {
                    var pubOpt = sharedClientService.readPublicKeyFromFile(Path.of("confidential/regionServerPublicKey.txt"));
                    if (pubOpt.isPresent()) {
                        var base64Opt = sharedClientService.toBase64PublicKey(pubOpt.get());
                        base64Opt.ifPresent(b64 -> sharedClientService.createKey(universeBaseUrl, "REGION", "PUBLIC", pubOpt.get().getAlgorithm(), regionServerId, KeyIntent.MAIN_JWT_TOKEN, regionServerId+":"+KeyIntent.MAIN_JWT_TOKEN+":"+UUID.randomUUID(), b64));
                    }
                }
            } catch (Exception ignore) {}
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

        // get region from database
        var region = regionService.getById(regionId);
        if (!region.isPresent() || !region.get().isEnabled()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        // validate token
        var token = auth.substring(7).trim();
        var claims = jwtService.validateTokenWithPublicKey(token, KeyType.REGION, KeyIntent.of(regionId, KeyIntent.MAIN_JWT_TOKEN));
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


}
