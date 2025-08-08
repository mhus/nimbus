package de.mhus.nimbus.server.shared.util;

import de.mhus.nimbus.server.shared.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthorizationUtils class.
 */
class AuthorizationUtilsTest {

    @Nested
    @DisplayName("Role Constants Tests")
    class RoleConstantsTests {

        @Test
        @DisplayName("Should have correct role constants")
        void shouldHaveCorrectRoleConstants() {
            assertEquals("ADMIN", AuthorizationUtils.Roles.ADMIN);
            assertEquals("USER", AuthorizationUtils.Roles.USER);
            assertEquals("CREATOR", AuthorizationUtils.Roles.CREATOR);
            assertEquals("MODERATOR", AuthorizationUtils.Roles.MODERATOR);
        }

        @Test
        @DisplayName("Should return all known roles")
        void shouldReturnAllKnownRoles() {
            List<String> knownRoles = AuthorizationUtils.getAllKnownRoles();

            assertEquals(4, knownRoles.size());
            assertTrue(knownRoles.contains("ADMIN"));
            assertTrue(knownRoles.contains("USER"));
            assertTrue(knownRoles.contains("CREATOR"));
            assertTrue(knownRoles.contains("MODERATOR"));
        }
    }

    @Nested
    @DisplayName("hasAnyRole Tests")
    class HasAnyRoleTests {

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AuthorizationUtils.hasAnyRole(null, "USER")
            );
            assertEquals("User cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should return true when no roles required")
        void shouldReturnTrueWhenNoRolesRequired() {
            UserDto user = createUser("test", List.of("USER"));

            assertTrue(AuthorizationUtils.hasAnyRole(user));
            assertTrue(AuthorizationUtils.hasAnyRole(user, (String[]) null));
        }

        @Test
        @DisplayName("Should return false when user has no roles")
        void shouldReturnFalseWhenUserHasNoRoles() {
            UserDto user = createUser("test", null);

            assertFalse(AuthorizationUtils.hasAnyRole(user, "USER"));

            user = createUser("test", Collections.emptyList());
            assertFalse(AuthorizationUtils.hasAnyRole(user, "USER"));
        }

        @Test
        @DisplayName("Should return true when ADMIN role is present")
        void shouldReturnTrueWhenAdminRolePresent() {
            UserDto adminUser = createUser("admin", List.of("ADMIN"));

            // Admin should have access regardless of required roles
            assertTrue(AuthorizationUtils.hasAnyRole(adminUser, "USER"));
            assertTrue(AuthorizationUtils.hasAnyRole(adminUser, "CREATOR"));
            assertTrue(AuthorizationUtils.hasAnyRole(adminUser, "MODERATOR"));
            assertTrue(AuthorizationUtils.hasAnyRole(adminUser, "UNKNOWN_ROLE"));
        }

        @Test
        @DisplayName("Should handle case insensitive role matching")
        void shouldHandleCaseInsensitiveRoleMatching() {
            UserDto user = createUser("test", List.of("user", "Creator"));

            assertTrue(AuthorizationUtils.hasAnyRole(user, "USER"));
            assertTrue(AuthorizationUtils.hasAnyRole(user, "user"));
            assertTrue(AuthorizationUtils.hasAnyRole(user, "CREATOR"));
            assertTrue(AuthorizationUtils.hasAnyRole(user, "creator"));
        }

        @Test
        @DisplayName("Should return true when user has one of multiple required roles")
        void shouldReturnTrueWhenUserHasOneOfMultipleRequiredRoles() {
            UserDto user = createUser("test", List.of("USER", "CREATOR"));

            assertTrue(AuthorizationUtils.hasAnyRole(user, "USER", "MODERATOR"));
            assertTrue(AuthorizationUtils.hasAnyRole(user, "MODERATOR", "CREATOR"));
            assertTrue(AuthorizationUtils.hasAnyRole(user, "ADMIN", "USER"));
        }

        @Test
        @DisplayName("Should return false when user has none of required roles")
        void shouldReturnFalseWhenUserHasNoneOfRequiredRoles() {
            UserDto user = createUser("test", List.of("USER"));

            assertFalse(AuthorizationUtils.hasAnyRole(user, "ADMIN"));
            assertFalse(AuthorizationUtils.hasAnyRole(user, "CREATOR", "MODERATOR"));
        }

        @Test
        @DisplayName("Should handle null roles in user role list")
        void shouldHandleNullRolesInUserRoleList() {
            UserDto user = createUser("test", Arrays.asList("USER", null, "CREATOR"));

            assertTrue(AuthorizationUtils.hasAnyRole(user, "USER"));
            assertTrue(AuthorizationUtils.hasAnyRole(user, "CREATOR"));
            assertFalse(AuthorizationUtils.hasAnyRole(user, "ADMIN"));
        }
    }

    @Nested
    @DisplayName("Specific Role Check Tests")
    class SpecificRoleCheckTests {

        @Test
        @DisplayName("hasRole should work correctly")
        void hasRoleShouldWorkCorrectly() {
            UserDto user = createUser("test", List.of("USER", "CREATOR"));

            assertTrue(AuthorizationUtils.hasRole(user, "USER"));
            assertTrue(AuthorizationUtils.hasRole(user, "CREATOR"));
            assertFalse(AuthorizationUtils.hasRole(user, "ADMIN"));
        }

        @Test
        @DisplayName("isAdmin should identify admin users")
        void isAdminShouldIdentifyAdminUsers() {
            UserDto adminUser = createUser("admin", List.of("ADMIN", "USER"));
            UserDto regularUser = createUser("user", List.of("USER"));

            assertTrue(AuthorizationUtils.isAdmin(adminUser));
            assertFalse(AuthorizationUtils.isAdmin(regularUser));
        }

        @Test
        @DisplayName("isUser should identify regular users")
        void isUserShouldIdentifyRegularUsers() {
            UserDto userWithUserRole = createUser("user", List.of("USER"));
            UserDto adminUser = createUser("admin", List.of("ADMIN"));
            UserDto creatorUser = createUser("creator", List.of("CREATOR"));

            assertTrue(AuthorizationUtils.isUser(userWithUserRole));
            assertTrue(AuthorizationUtils.isUser(adminUser)); // Admin also has USER access
            assertFalse(AuthorizationUtils.isUser(creatorUser));
        }

        @Test
        @DisplayName("isCreator should identify creator users")
        void isCreatorShouldIdentifyCreatorUsers() {
            UserDto creatorUser = createUser("creator", List.of("CREATOR"));
            UserDto adminUser = createUser("admin", List.of("ADMIN"));
            UserDto regularUser = createUser("user", List.of("USER"));

            assertTrue(AuthorizationUtils.isCreator(creatorUser));
            assertTrue(AuthorizationUtils.isCreator(adminUser)); // Admin has all access
            assertFalse(AuthorizationUtils.isCreator(regularUser));
        }

        @Test
        @DisplayName("isModerator should identify moderator users")
        void isModeratorShouldIdentifyModeratorUsers() {
            UserDto moderatorUser = createUser("moderator", List.of("MODERATOR"));
            UserDto adminUser = createUser("admin", List.of("ADMIN"));
            UserDto regularUser = createUser("user", List.of("USER"));

            assertTrue(AuthorizationUtils.isModerator(moderatorUser));
            assertTrue(AuthorizationUtils.isModerator(adminUser)); // Admin has all access
            assertFalse(AuthorizationUtils.isModerator(regularUser));
        }
    }

    @Nested
    @DisplayName("canAccessUserData Tests")
    class CanAccessUserDataTests {

        @Test
        @DisplayName("Should throw exception when current user is null")
        void shouldThrowExceptionWhenCurrentUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AuthorizationUtils.canAccessUserData(null, "target")
            );
            assertEquals("Current user cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Admin should access all user data")
        void adminShouldAccessAllUserData() {
            UserDto adminUser = createUser("admin", List.of("ADMIN"));

            assertTrue(AuthorizationUtils.canAccessUserData(adminUser, "user1"));
            assertTrue(AuthorizationUtils.canAccessUserData(adminUser, "user2"));
            assertTrue(AuthorizationUtils.canAccessUserData(adminUser, "admin"));
        }

        @Test
        @DisplayName("Users should access only their own data")
        void usersShouldAccessOnlyTheirOwnData() {
            UserDto user1 = createUser("user1", List.of("USER"));
            UserDto user2 = createUser("user2", List.of("USER"));

            assertTrue(AuthorizationUtils.canAccessUserData(user1, "user1"));
            assertFalse(AuthorizationUtils.canAccessUserData(user1, "user2"));

            assertTrue(AuthorizationUtils.canAccessUserData(user2, "user2"));
            assertFalse(AuthorizationUtils.canAccessUserData(user2, "user1"));
        }

        @Test
        @DisplayName("Should handle null user ID")
        void shouldHandleNullUserId() {
            UserDto userWithNullId = createUser(null, List.of("USER"));

            assertFalse(AuthorizationUtils.canAccessUserData(userWithNullId, "target"));
            assertFalse(AuthorizationUtils.canAccessUserData(userWithNullId, null));
        }
    }

    /**
     * Helper method to create a UserDto for testing.
     */
    private UserDto createUser(String id, List<String> roles) {
        return UserDto.builder()
                .id(id)
                .name("Test User")
                .email("test@example.com")
                .roles(roles)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
    }
}
