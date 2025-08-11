package de.mhus.nimbus.worldshared.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Authentication context for world services.
 * Contains user information that is passed between services.
 */
@Getter
@Builder
public class WorldAuthContext {

    /**
     * The authenticated username.
     */
    private final String username;

    /**
     * The user's roles.
     */
    private final List<String> roles;

    /**
     * Whether the request is authenticated.
     */
    private final boolean authenticated;

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return authenticated && roles != null && roles.contains(role);
    }

    /**
     * Checks if the user has any of the specified roles.
     *
     * @param roles the roles to check
     * @return true if the user has any of the roles, false otherwise
     */
    public boolean hasAnyRole(String... roles) {
        if (!authenticated || this.roles == null) {
            return false;
        }

        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
