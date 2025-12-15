package de.mhus.nimbus.world.player.security;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.world.shared.access.AccessFilterBase;
import de.mhus.nimbus.world.shared.session.WSessionService;
import org.springframework.stereotype.Component;

/**
 * Access filter for world-player service.
 * Extends AccessFilterBase to validate sessionToken cookies and attach
 * session information to requests.
 */
@Component
public class PlayerAccessFilter extends AccessFilterBase {

    public PlayerAccessFilter(JwtService jwtService, WSessionService sessionService) {
        super(jwtService, sessionService);
    }
}
