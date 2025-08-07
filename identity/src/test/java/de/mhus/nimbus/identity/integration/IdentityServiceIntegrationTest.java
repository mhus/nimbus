package de.mhus.nimbus.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import de.mhus.nimbus.identity.service.IdentityService;
import de.mhus.nimbus.identity.util.JwtTokenUtils;
import de.mhus.nimbus.server.shared.dto.*;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Identity Service REST endpoints.
 * Tests the complete flow from HTTP request to database operations.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class IdentityServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdentityService identityService;

    @MockBean
    private IdentityServiceUtils identityServiceUtils;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User testUser;
    private CreateUserDto createUserDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean database
        userRepository.deleteAll();

        // Setup test data
        testUser = User.builder()
                .id("testuser")
                .name("Test User")
                .nickname("tester")
                .email("test@example.com")
                .roles(Arrays.asList("USER"))
                .passwordHash("hashedpassword")
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .build();

        createUserDto = CreateUserDto.builder()
                .id("newuser")
                .name("New User")
                .nickname("new")
                .email("new@example.com")
                .password("password123")
                .roles(Arrays.asList("USER"))
                .build();

        // Setup mocks
        when(identityServiceUtils.hashPassword(anyString(), anyString())).thenReturn("hashedpassword");
        when(identityServiceUtils.verifyPassword(anyString(), anyString(), anyString())).thenReturn(true);
        when(jwtTokenUtils.createToken(anyString(), anyList())).thenReturn("jwt.token.here");
        when(jwtTokenUtils.getExpirationTime(anyString())).thenReturn(System.currentTimeMillis() / 1000 + 7200);
        when(jwtTokenUtils.getIssuedTime(anyString())).thenReturn(System.currentTimeMillis() / 1000);
    }

    @Test
    void createUser_EndToEnd_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("newuser"))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        // Verify database state
        assertTrue(userRepository.existsById("newuser"));
        User savedUser = userRepository.findById("newuser").orElseThrow();
        assertEquals("New User", savedUser.getName());
        assertEquals("new@example.com", savedUser.getEmail());
    }

    @Test
    void createUser_DuplicateId_BadRequest() throws Exception {
        // Given - save user first
        userRepository.save(testUser);

        CreateUserDto duplicateDto = CreateUserDto.builder()
                .id("testuser") // Same ID as existing user
                .name("Duplicate User")
                .nickname("duplicate")
                .email("duplicate@example.com")
                .password("password123")
                .roles(Arrays.asList("USER"))
                .build();

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_WithValidAuthorization_Success() throws Exception {
        // Given
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(get("/users/testuser")
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserById_AdminAccess_Success() throws Exception {
        // Given
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(get("/users/testuser")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"));
    }

    @Test
    void getUserById_UnauthorizedAccess_Forbidden() throws Exception {
        // Given
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(get("/users/testuser")
                        .requestAttr("userId", "otheruser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_EndToEnd_Success() throws Exception {
        // Given
        userRepository.save(testUser);

        UserDto updateDto = UserDto.builder()
                .id("testuser")
                .name("Updated Name")
                .nickname("updated")
                .email("updated@example.com")
                .roles(Arrays.asList("USER", "MODERATOR"))
                .build();

        // When & Then
        mockMvc.perform(put("/users/testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        // Verify database state
        User updatedUser = userRepository.findById("testuser").orElseThrow();
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals(2, updatedUser.getRoles().size());
    }

    @Test
    void deleteUser_AdminAccess_Success() throws Exception {
        // Given
        userRepository.save(testUser);
        assertTrue(userRepository.existsById("testuser"));

        // When & Then
        mockMvc.perform(delete("/users/testuser")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isNoContent());

        // Verify database state
        assertFalse(userRepository.existsById("testuser"));
    }

    @Test
    void deleteUser_NonAdminAccess_Forbidden() throws Exception {
        // Given
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(delete("/users/testuser")
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        // Verify user still exists
        assertTrue(userRepository.existsById("testuser"));
    }

    @Test
    void getAllUsers_AdminAccess_Success() throws Exception {
        // Given
        userRepository.save(testUser);
        User secondUser = User.builder()
                .id("seconduser")
                .name("Second User")
                .nickname("second")
                .email("second@example.com")
                .roles(Arrays.asList("ADMIN"))
                .passwordHash("hashedpassword2")
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .build();
        userRepository.save(secondUser);

        // When & Then
        mockMvc.perform(get("/users")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void login_ValidCredentials_Success() throws Exception {
        // Given
        userRepository.save(testUser);

        LoginDto loginDto = LoginDto.builder()
                .userId("testuser")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.issuedAt").exists());
    }

    @Test
    void login_InvalidCredentials_Unauthorized() throws Exception {
        // Given
        when(identityServiceUtils.verifyPassword(anyString(), anyString(), anyString())).thenReturn(false);

        LoginDto invalidLoginDto = LoginDto.builder()
                .userId("testuser")
                .password("wrongpassword")
                .build();

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_ValidOldPassword_Success() throws Exception {
        // Given
        userRepository.save(testUser);

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .oldPassword("oldpassword")
                .newPassword("newpassword")
                .build();

        // When & Then
        mockMvc.perform(post("/users/testuser/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_InvalidOldPassword_BadRequest() throws Exception {
        // Given
        userRepository.save(testUser);
        when(identityServiceUtils.verifyPassword(anyString(), anyString(), anyString())).thenReturn(false);

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .oldPassword("wrongpassword")
                .newPassword("newpassword")
                .build();

        // When & Then
        mockMvc.perform(post("/users/testuser/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renewToken_ValidToken_Success() throws Exception {
        // Given
        when(identityServiceUtils.extractUserId("old.token")).thenReturn("testuser");
        when(identityServiceUtils.extractRoles("old.token")).thenReturn(Arrays.asList("USER"));
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(post("/token/renew")
                        .header("Authorization", "Bearer old.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    void renewToken_InvalidToken_Unauthorized() throws Exception {
        // Given
        when(identityServiceUtils.extractUserId("invalid.token")).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/token/renew")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthCheck_Always_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Identity Service is running"));
    }

    @Test
    void multipleOperations_UserLifecycle_Success() throws Exception {
        // 1. Create user
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isCreated());

        // 2. Login with new user
        LoginDto loginDto = LoginDto.builder()
                .userId("newuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk());

        // 3. Get user info
        mockMvc.perform(get("/users/newuser")
                        .requestAttr("userId", "newuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("newuser"));

        // 4. Update user
        UserDto updateDto = UserDto.builder()
                .id("newuser")
                .name("Updated New User")
                .nickname("updated")
                .email("updated.new@example.com")
                .roles(Arrays.asList("USER"))
                .build();

        mockMvc.perform(put("/users/newuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .requestAttr("userId", "newuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated New User"));

        // 5. Change password
        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .oldPassword("password123")
                .newPassword("newpassword123")
                .build();

        mockMvc.perform(post("/users/newuser/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto))
                        .requestAttr("userId", "newuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk());

        // 6. Delete user (as admin)
        mockMvc.perform(delete("/users/newuser")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isNoContent());

        // Verify user is deleted
        assertFalse(userRepository.existsById("newuser"));
    }
}
