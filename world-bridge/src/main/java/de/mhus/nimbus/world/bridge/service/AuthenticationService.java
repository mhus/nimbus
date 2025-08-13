package de.mhus.nimbus.world.bridge.service;

import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    private final IdentityServiceUtils identityServiceUtils = new IdentityServiceUtils();

    public AuthenticationResult validateToken(String token) {
        try {
            DecodedJWT decodedJWT = identityServiceUtils.validateToken(token);

            if (decodedJWT != null) {
                String userId = decodedJWT.getSubject();
                List<String> rolesList = decodedJWT.getClaim("roles").asList(String.class);
                Set<String> roles = rolesList != null ? Set.copyOf(rolesList) : Set.of();
                String username = decodedJWT.getClaim("username").asString();

                return new AuthenticationResult(true, userId, roles, username);
            }

        } catch (Exception e) {
            log.error("Error validating token", e);
        }

        return new AuthenticationResult(false, null, Set.of(), null);
    }
}
