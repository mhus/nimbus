package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import de.mhus.nimbus.identity.util.JwtTokenUtils;
import de.mhus.nimbus.server.shared.dto.UserDto;
import de.mhus.nimbus.server.shared.util.AuthorizationUtils;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for verifying that users with ADMIN role get all known roles assigned
 * when returned from the IdentityService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Role Expansion Tests")
class AdminRoleExpansionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdentityServiceUtils identityServiceUtils;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private IdentityService identityService;

    private User adminUser;
    private User regularUser;
    private User userWithMultipleRoles;

    @BeforeEach
    void setUp() {
        long now = Instant.now().toEpochMilli();

        // Create admin user with only ADMIN role
        adminUser = User.builder()
                .id("admin")
                .name("Administrator")
                .nickname("admin")
                .email("admin@test.com")
                .roles(List.of("ADMIN"))
                .passwordHash("hashedPassword")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Create regular user with only USER role
        regularUser = User.builder()
                .id("user")
                .name("Regular User")
                .nickname("user")
                .email("user@test.com")
                .roles(List.of("USER"))
                .passwordHash("hashedPassword")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Create user with multiple roles including ADMIN
        userWithMultipleRoles = User.builder()
                .id("superuser")
                .name("Super User")
                .nickname("super")
                .email("super@test.com")
                .roles(List.of("ADMIN", "CREATOR"))
                .passwordHash("hashedPassword")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("Admin user should get all known roles when retrieved")
    void getUser_WithAdminRole_ShouldReturnAllKnownRoles() {
        // Given
        when(userRepository.findById("admin")).thenReturn(Optional.of(adminUser));

        // When
        UserDto result = identityService.getUser("admin");

        // Then
        assertNotNull(result);
        assertEquals("admin", result.getId());
        assertEquals("Administrator", result.getName());

        // Verify all known roles are present
        List<String> expectedRoles = AuthorizationUtils.getAllKnownRoles();
        assertEquals(expectedRoles.size(), result.getRoles().size());
        assertTrue(result.getRoles().containsAll(expectedRoles));

        verify(userRepository).findById("admin");
    }

    @Test
    @DisplayName("Regular user should keep their original roles")
    void getUser_WithoutAdminRole_ShouldKeepOriginalRoles() {
        // Given
        when(userRepository.findById("user")).thenReturn(Optional.of(regularUser));

        // When
        UserDto result = identityService.getUser("user");

        // Then
        assertNotNull(result);
        assertEquals("user", result.getId());
        assertEquals("Regular User", result.getName());

        // Verify original roles are preserved
        assertEquals(1, result.getRoles().size());
        assertEquals("USER", result.getRoles().get(0));

        verify(userRepository).findById("user");
    }

    @Test
    @DisplayName("User with ADMIN and other roles should get all known roles")
    void getUser_WithAdminAndOtherRoles_ShouldReturnAllKnownRoles() {
        // Given
        when(userRepository.findById("superuser")).thenReturn(Optional.of(userWithMultipleRoles));

        // When
        UserDto result = identityService.getUser("superuser");

        // Then
        assertNotNull(result);
        assertEquals("superuser", result.getId());

        // Verify all known roles are present (not just original ones)
        List<String> expectedRoles = AuthorizationUtils.getAllKnownRoles();
        assertEquals(expectedRoles.size(), result.getRoles().size());
        assertTrue(result.getRoles().containsAll(expectedRoles));

        verify(userRepository).findById("superuser");
    }

    @Test
    @DisplayName("Case-insensitive ADMIN role detection should work")
    void getUser_WithLowercaseAdminRole_ShouldReturnAllKnownRoles() {
        // Given
        User lowercaseAdminUser = User.builder()
                .id("lowercaseadmin")
                .name("Lowercase Admin")
                .nickname("ladmin")
                .email("ladmin@test.com")
                .roles(List.of("admin"))  // lowercase admin
                .passwordHash("hashedPassword")
                .createdAt(Instant.now().toEpochMilli())
                .updatedAt(Instant.now().toEpochMilli())
                .build();

        when(userRepository.findById("lowercaseadmin")).thenReturn(Optional.of(lowercaseAdminUser));

        // When
        UserDto result = identityService.getUser("lowercaseadmin");

        // Then
        assertNotNull(result);
        assertEquals("lowercaseadmin", result.getId());

        // Verify all known roles are present despite lowercase "admin"
        List<String> expectedRoles = AuthorizationUtils.getAllKnownRoles();
        assertEquals(expectedRoles.size(), result.getRoles().size());
        assertTrue(result.getRoles().containsAll(expectedRoles));

        verify(userRepository).findById("lowercaseadmin");
    }

    @Test
    @DisplayName("GetAllUsers should expand admin roles for all admin users")
    void getAllUsers_WithMixedUsers_ShouldExpandAdminRolesOnly() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(adminUser, regularUser, userWithMultipleRoles));

        // When
        List<UserDto> result = identityService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Find admin users in result
        UserDto adminResult = result.stream()
                .filter(u -> "admin".equals(u.getId()))
                .findFirst()
                .orElseThrow();

        UserDto superuserResult = result.stream()
                .filter(u -> "superuser".equals(u.getId()))
                .findFirst()
                .orElseThrow();

        UserDto userResult = result.stream()
                .filter(u -> "user".equals(u.getId()))
                .findFirst()
                .orElseThrow();

        // Verify admin users have all roles
        List<String> expectedRoles = AuthorizationUtils.getAllKnownRoles();
        assertTrue(adminResult.getRoles().containsAll(expectedRoles));
        assertTrue(superuserResult.getRoles().containsAll(expectedRoles));

        // Verify regular user keeps original roles
        assertEquals(List.of("USER"), userResult.getRoles());

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("User with null roles should not cause errors")
    void getUser_WithNullRoles_ShouldNotThrowException() {
        // Given
        User userWithNullRoles = User.builder()
                .id("nulluser")
                .name("Null Roles User")
                .nickname("null")
                .email("null@test.com")
                .roles(null)  // null roles
                .passwordHash("hashedPassword")
                .createdAt(Instant.now().toEpochMilli())
                .updatedAt(Instant.now().toEpochMilli())
                .build();

        when(userRepository.findById("nulluser")).thenReturn(Optional.of(userWithNullRoles));

        // When & Then
        assertDoesNotThrow(() -> {
            UserDto result = identityService.getUser("nulluser");
            assertNotNull(result);
            assertEquals("nulluser", result.getId());
            assertNull(result.getRoles()); // Should remain null
        });

        verify(userRepository).findById("nulluser");
    }
}
