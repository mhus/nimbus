package de.mhus.nimbus.server.shared.util;

import de.mhus.nimbus.server.shared.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for user authorization and role checking.
 * Provides methods to validate user permissions based on roles.
 */
@Slf4j
public class AuthorizationUtils {

    /**
     * Known system roles as constants.
     * All roles are stored in uppercase for consistency.
     */
    public static final class Roles {
        /** Administrator role with full system access */
        public static final String ADMIN = "ADMIN";

        /** Basic user role */
        public static final String USER = "USER";

        /** Content creator role - can create content but no admin rights */
        public static final String CREATOR = "CREATOR";

        /** Content moderator role - can moderate content but no admin rights */
        public static final String MODERATOR = "MODERATOR";

        private Roles() {
            // Utility class - prevent instantiation
        }
    }

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private AuthorizationUtils() {
        // Utility class
    }

    /**
     * Checks if a user has any of the specified roles.
     *
     * @param user The user to check (must not be null)
     * @param requiredRoles One or more roles to check for (varargs)
     * @return true if user has at least one of the required roles, false otherwise
     * @throws IllegalArgumentException if user is null
     */
    public static boolean hasAnyRole(UserDto user, String... requiredRoles) {
        if (user == null) {
            log.warn("Authorization check failed: user is null");
            throw new IllegalArgumentException("User cannot be null");
        }

        if (requiredRoles == null || requiredRoles.length == 0) {
            log.debug("No roles specified for authorization check - allowing access");
            return true;
        }

        List<String> userRoles = user.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            log.debug("User {} has no roles assigned", user.getId());
            return false;
        }

        // Convert user roles to uppercase for case-insensitive comparison
        List<String> normalizedUserRoles = userRoles.stream()
                .map(role -> role != null ? role.toUpperCase() : "")
                .toList();

        // ADMIN role always grants access
        if (normalizedUserRoles.contains(Roles.ADMIN)) {
            log.debug("User {} has ADMIN role - granting access", user.getId());
            return true;
        }

        // Check if user has any of the required roles (case-insensitive)
        List<String> normalizedRequiredRoles = Arrays.stream(requiredRoles)
                .map(role -> role != null ? role.toUpperCase() : "")
                .toList();

        boolean hasRole = normalizedUserRoles.stream()
                .anyMatch(normalizedRequiredRoles::contains);

        if (hasRole) {
            log.debug("User {} has required role(s) - granting access", user.getId());
        } else {
            log.debug("User {} does not have any required role(s): {}. User roles: {}",
                    user.getId(), Arrays.toString(requiredRoles), userRoles);
        }

        return hasRole;
    }

    /**
     * Checks if a user has a specific role.
     *
     * @param user The user to check (must not be null)
     * @param requiredRole The role to check for
     * @return true if user has the specified role, false otherwise
     * @throws IllegalArgumentException if user is null
     */
    public static boolean hasRole(UserDto user, String requiredRole) {
        return hasAnyRole(user, requiredRole);
    }

    /**
     * Checks if a user has the ADMIN role.
     *
     * @param user The user to check (must not be null)
     * @return true if user is an admin, false otherwise
     * @throws IllegalArgumentException if user is null
     */
    public static boolean isAdmin(UserDto user) {
        return hasRole(user, Roles.ADMIN);
    }

    /**
     * Checks if a user has the USER role.
     *
     * @param user The user to check (must not be null)
     * @return true if user has USER role, false otherwise
     * @throws IllegalArgumentException if user is null
     */
    public static boolean isUser(UserDto user) {
        return hasRole(user, Roles.USER);
    }

    /**
     * Checks if a user has the CREATOR role.
     *
     * @param user The user to check (must not be null)
     * @return true if user has CREATOR role, false otherwise
     * @throws IllegalArgumentException if user is null
     */
    public static boolean isCreator(UserDto user) {
        return hasRole(user, Roles.CREATOR);
    }

    /**
     * Checks if a user has the MODERATOR role.
     *
     * @param user The user to check (must not be null)
     * @return true if user has MODERATOR role, false otherwise
     * @throws IllegalArgumentException if user is null
     */
    public static boolean isModerator(UserDto user) {
        return hasRole(user, Roles.MODERATOR);
    }

    /**
     * Checks if a user can access another user's data.
     * Users can always access their own data, admins can access all data.
     *
     * @param currentUser The user trying to access data
     * @param targetUserId The ID of the user whose data is being accessed
     * @return true if access is allowed, false otherwise
     * @throws IllegalArgumentException if currentUser is null
     */
    public static boolean canAccessUserData(UserDto currentUser, String targetUserId) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }

        // Admin can access all data
        if (isAdmin(currentUser)) {
            return true;
        }

        // Users can access their own data
        return currentUser.getId() != null && currentUser.getId().equals(targetUserId);
    }

    /**
     * Gets all known system roles.
     *
     * @return List of all predefined system roles
     */
    public static List<String> getAllKnownRoles() {
        return List.of(Roles.ADMIN, Roles.USER, Roles.CREATOR, Roles.MODERATOR);
    }

    /**
     * Extracts the user ID from the HTTP request.
     * The user ID is set by the JWT authentication filter as a request attribute.
     *
     * @param request The HTTP servlet request
     * @return The user ID, or null if not found
     */
    public static String getUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userId = request.getAttribute("userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * Extracts the user roles from the HTTP request.
     * The user roles are set by the JWT authentication filter as a request attribute.
     *
     * @param request The HTTP servlet request
     * @return The list of user roles, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static List<String> getUserRoles(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object roles = request.getAttribute("userRoles");
        if (roles instanceof List<?>) {
            try {
                return (List<String>) roles;
            } catch (ClassCastException e) {
                log.warn("User roles attribute is not a List<String>: {}", roles.getClass());
                return null;
            }
        }
        return null;
    }
}
