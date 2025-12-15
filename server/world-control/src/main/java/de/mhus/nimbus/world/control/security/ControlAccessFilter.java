package de.mhus.nimbus.world.control.security;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.world.shared.access.AccessFilterBase;
import de.mhus.nimbus.world.shared.session.WSessionService;
import org.springframework.stereotype.Component;

/**
 * Access filter for world-control service.
 * Extends AccessFilterBase to validate sessionToken cookies and attach
 * session information to requests.
 */
@Component
public class ControlAccessFilter extends AccessFilterBase {

    public ControlAccessFilter(JwtService jwtService, WSessionService sessionService) {
        super(jwtService, sessionService);
    }
}
