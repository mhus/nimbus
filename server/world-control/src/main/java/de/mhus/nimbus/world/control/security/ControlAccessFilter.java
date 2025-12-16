package de.mhus.nimbus.world.control.security;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.world.shared.access.AccessFilterBase;
import de.mhus.nimbus.world.shared.access.AccessProperties;
import de.mhus.nimbus.world.shared.region.RegionProperties;
import de.mhus.nimbus.world.shared.session.WSessionService;
import org.springframework.stereotype.Component;

/**
 * Access filter for world-control service.
 * Extends AccessFilterBase to validate sessionToken cookies and attach
 * session information to requests.
 *
 * Enforces authentication for all endpoints except:
 * - /api/aaa/authorize (cookie setting endpoint)
 * - /api/aaa/devlogin (development login endpoint)
 * - /control/aaa/status (status check endpoint)
 * - /control/aaa/login DELETE (logout endpoint)
 */
@Component
public class ControlAccessFilter extends AccessFilterBase {

    private final AccessProperties accessProperties;

    public ControlAccessFilter(JwtService jwtService, WSessionService sessionService, AccessProperties accessProperties, RegionProperties regionProperties) {
        super(jwtService, sessionService, regionProperties);
        this.accessProperties = accessProperties;
    }

    @Override
    protected boolean shouldRequireAuthentication(String requestUri, String method) {
        // Allow access to authentication-related endpoints
        if (requestUri.startsWith("/control/aaa/authorize")) {
            return false;
        }
        if (requestUri.startsWith("/control/aaa/devlogin")) {
            return false;
        }
        if (requestUri.startsWith("/control/aaa/status")) {
            return false;
        }
//        // Allow DELETE on /control/aaa/login (logout)
//        if (requestUri.startsWith("/control/aaa/login") && "DELETE".equals(method)) {
//            return false;
//        }

        // All other endpoints require authentication
        return true;
    }

    @Override
    protected String getLoginUrl() {
        return accessProperties.getLoginUrl();
    }
}
