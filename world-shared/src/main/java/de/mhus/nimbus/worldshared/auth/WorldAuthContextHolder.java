package de.mhus.nimbus.worldshared.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper class to access the current authentication context from anywhere in the application.
 */
public class WorldAuthContextHolder {

    /**
     * Gets the current authentication context from the request.
     *
     * @return the authentication context, or null if not available
     */
    public static WorldAuthContext getCurrentContext() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return (WorldAuthContext) request.getAttribute(WorldAuthorizationFilter.AUTH_CONTEXT_ATTRIBUTE);
    }

    /**
     * Gets the current authenticated username.
     *
     * @return the username, or null if not authenticated
     */
    public static String getCurrentUsername() {
        WorldAuthContext context = getCurrentContext();
        return context != null && context.isAuthenticated() ? context.getUsername() : null;
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        WorldAuthContext context = getCurrentContext();
        return context != null && context.isAuthenticated();
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        WorldAuthContext context = getCurrentContext();
        return context != null && context.hasRole(role);
    }

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param roles the roles to check
     * @return true if the user has any of the roles, false otherwise
     */
    public static boolean hasAnyRole(String... roles) {
        WorldAuthContext context = getCurrentContext();
        return context != null && context.hasAnyRole(roles);
    }
}
