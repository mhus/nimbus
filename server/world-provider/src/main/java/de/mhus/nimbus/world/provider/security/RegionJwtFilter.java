package de.mhus.nimbus.world.provider.security;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.world.shared.world.WWorldService; // falls spätere Prüfung benötigt
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Prüft für Pfade /world/region/{regionId}/** ein gültiges Region-JWT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegionJwtFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/world/region/";

    private final JwtService jwtService;

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
        var claimsOpt = jwtService.validateTokenWithPublicKey(token, KeyType.REGION, KeyIntent.of(regionId, KeyIntent.MAIN_JWT_TOKEN));
        if (claimsOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        // Optional: refresh claims checks, region enabled check etc.
        filterChain.doFilter(request, response);
    }

    private boolean applies(String path) {
        return path != null && path.startsWith(PREFIX);
    }

    private String extractRegionId(String path) {
        if (path == null) return null;
        String rest = path.substring(PREFIX.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}

