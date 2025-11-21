package de.mhus.nimbus.region.security;

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
 * Prüft für Aufrufe unter /region/{regionId}/world/{worldId} ein Bearer-Token,
 * das mit dem WORLD-Key signiert wurde. Dabei ist der Owner des KeyIds die worldId
 * und die UUID wird aus RegionJwtProperties.worldKeyUuid entnommen.
 */
@Component
public class RegionWorldJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASE_PREFIX = "/region/"; // erwartet .../region/{regionId}/world/{worldId}

    private final JwtService jwtService;
    private final RegionJwtProperties props;

    public RegionWorldJwtAuthenticationFilter(JwtService jwtService, RegionJwtProperties props) {
        this.jwtService = jwtService;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!applies(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String worldId = extractWorldId(path);
        if (worldId == null || worldId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = auth.substring(7).trim();

        String uuid = props.getWorldKeyUuid();
        if (uuid == null || uuid.isBlank()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String keyId = worldId + ":" + uuid.trim();

        Optional<Jws<Claims>> claims = jwtService.validateTokenWithSecretKey(token, keyId);
        if (claims.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean applies(String path) {
        if (path == null || !path.startsWith(BASE_PREFIX)) return false;
        // wir interessieren uns nur für /region/{regionId}/world/
        int idx = path.indexOf("/world/");
        return idx > 0; // enthält Segment /world/
    }

    private String extractWorldId(String path) {
        // erwartetes Muster: /region/{regionId}/world/{worldId}[...]
        int pivot = path.indexOf("/world/");
        if (pivot < 0) return null;
        String rest = path.substring(pivot + "/world/".length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
