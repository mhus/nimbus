package de.mhus.nimbus.identity.security;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import de.mhus.nimbus.server.shared.filter.JWTAuthenticationFilter;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authorization and role-based access control.
 * Tests that different user roles have appropriate access to endpoints.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class AuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private IdentityServiceUtils identityServiceUtils;

    private MockMvc mockMvc;
    private User adminUser;
    private User regularUser;
    private User moderatorUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean database
        userRepository.deleteAll();

        // Setup test users with different roles
        adminUser = User.builder()
                .id("admin")
                .name("Admin User")
                .nickname("admin")
                .email("admin@example.com")
                .roles(Arrays.asList("ADMIN"))
                .passwordHash("hashedpassword")
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .build();

        regularUser = User.builder()
                .id("user")
                .name("Regular User")
                .nickname("user")
                .email("user@example.com")
                .roles(Arrays.asList("USER"))
                .passwordHash("hashedpassword")
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .build();

        moderatorUser = User.builder()
                .id("moderator")
                .name("Moderator User")
                .nickname("moderator")
                .email("moderator@example.com")
                .roles(Arrays.asList("USER", "MODERATOR"))
                .passwordHash("hashedpassword")
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .build();

        userRepository.save(adminUser);
        userRepository.save(regularUser);
        userRepository.save(moderatorUser);

        // Setup JWT token mocks
        when(identityServiceUtils.extractUserId("admin.token")).thenReturn("admin");
        when(identityServiceUtils.extractRoles("admin.token")).thenReturn(Arrays.asList("ADMIN"));

        when(identityServiceUtils.extractUserId("user.token")).thenReturn("user");
        when(identityServiceUtils.extractRoles("user.token")).thenReturn(Arrays.asList("USER"));

        when(identityServiceUtils.extractUserId("moderator.token")).thenReturn("moderator");
        when(identityServiceUtils.extractRoles("moderator.token")).thenReturn(Arrays.asList("USER", "MODERATOR"));
    }

    @Test
    void getAllUsers_OnlyAdminCanAccess() throws Exception {
        // Admin can access
        mockMvc.perform(get("/users")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk());

        // Regular user cannot access
        mockMvc.perform(get("/users")
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        // Moderator cannot access (only admin can list all users)
        mockMvc.perform(get("/users")
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_OwnerAndAdminCanAccess() throws Exception {
        // User can access their own data
        mockMvc.perform(get("/users/user")
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk());

        // Admin can access any user's data
        mockMvc.perform(get("/users/user")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk());

        // Different user cannot access other user's data
        mockMvc.perform(get("/users/user")
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_OwnerAndAdminCanUpdate() throws Exception {
        String userJson = """
                {
                    "id": "user",
                    "name": "Updated User",
                    "nickname": "updated",
                    "email": "updated@example.com",
                    "roles": ["USER"]
                }
                """;

        // User can update their own data
        mockMvc.perform(put("/users/user")
                        .contentType("application/json")
                        .content(userJson)
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk());

        // Admin can update any user's data
        mockMvc.perform(put("/users/user")
                        .contentType("application/json")
                        .content(userJson)
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk());

        // Different user cannot update other user's data
        mockMvc.perform(put("/users/user")
                        .contentType("application/json")
                        .content(userJson)
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_OnlyAdminCanDelete() throws Exception {
        // Admin can delete users
        mockMvc.perform(delete("/users/user")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isNoContent());

        // Regular user cannot delete (even their own account)
        mockMvc.perform(delete("/users/user")
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        // Moderator cannot delete users
        mockMvc.perform(delete("/users/user")
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_OwnerAndAdminCanChange() throws Exception {
        String changePasswordJson = """
                {
                    "oldPassword": "oldpass",
                    "newPassword": "newpass"
                }
                """;

        // User can change their own password
        mockMvc.perform(post("/users/user/change-password")
                        .contentType("application/json")
                        .content(changePasswordJson)
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk());

        // Admin can change any user's password
        mockMvc.perform(post("/users/user/change-password")
                        .contentType("application/json")
                        .content(changePasswordJson)
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk());

        // Different user cannot change other user's password
        mockMvc.perform(post("/users/user/change-password")
                        .contentType("application/json")
                        .content(changePasswordJson)
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_AllAuthenticatedUsersCanCreate() throws Exception {
        String newUserJson = """
                {
                    "id": "newuser",
                    "name": "New User",
                    "nickname": "new",
                    "email": "new@example.com",
                    "password": "password123",
                    "roles": ["USER"]
                }
                """;

        // Admin can create users
        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(newUserJson))
                .andExpect(status().isCreated());

        // Note: In a real scenario, you might want to restrict user creation
        // to admins only, but the current implementation allows any request
    }

    @Test
    void login_NoAuthenticationRequired() throws Exception {
        String loginJson = """
                {
                    "userId": "user",
                    "password": "password123"
                }
                """;

        // Login endpoint should not require authentication
        mockMvc.perform(post("/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isUnauthorized()); // Unauthorized due to wrong password, not forbidden
    }

    @Test
    void health_NoAuthenticationRequired() throws Exception {
        // Health endpoint should not require authentication
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Identity Service is running"));
    }

    @Test
    void multipleRoles_ModeratorUser_CorrectAccess() throws Exception {
        // Moderator (who also has USER role) can access their own data
        mockMvc.perform(get("/users/moderator")
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isOk());

        // Moderator can update their own data
        String moderatorJson = """
                {
                    "id": "moderator",
                    "name": "Updated Moderator",
                    "nickname": "mod",
                    "email": "mod@example.com",
                    "roles": ["USER", "MODERATOR"]
                }
                """;

        mockMvc.perform(put("/users/moderator")
                        .contentType("application/json")
                        .content(moderatorJson)
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isOk());

        // Moderator cannot delete users (still need ADMIN role)
        mockMvc.perform(delete("/users/user")
                        .requestAttr("userId", "moderator")
                        .requestAttr("userRoles", Arrays.asList("USER", "MODERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noRoles_EmptyRolesList_RestrictedAccess() throws Exception {
        // User with empty roles list should have limited access
        mockMvc.perform(get("/users/user")
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", Arrays.asList())) // Empty roles
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList())) // Empty roles, even for admin user
                .andExpect(status().isForbidden());
    }

    @Test
    void nullRoles_RestrictedAccess() throws Exception {
        // User with null roles should have no access
        mockMvc.perform(get("/users/user")
                        .requestAttr("userId", "user")
                        .requestAttr("userRoles", null))
                .andExpect(status().isForbidden());
    }

    @Test
    void caseSensitiveRoles_ShouldWork() throws Exception {
        // Test that role checking is case-sensitive
        mockMvc.perform(get("/users")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("admin"))) // lowercase
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN"))) // uppercase
                .andExpect(status().isOk());
    }
}
