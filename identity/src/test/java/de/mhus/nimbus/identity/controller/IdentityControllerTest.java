package de.mhus.nimbus.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.identity.service.IdentityService;
import de.mhus.nimbus.server.shared.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for IdentityController.
 * Tests all REST endpoints and authorization scenarios.
 */
@WebMvcTest(IdentityController.class)
public class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityService identityService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUserDto createUserDto;
    private UserDto userDto;
    private LoginDto loginDto;
    private TokenDto tokenDto;
    private ChangePasswordDto changePasswordDto;

    @BeforeEach
    void setUp() {
        createUserDto = CreateUserDto.builder()
                .id("testuser")
                .name("Test User")
                .nickname("tester")
                .email("test@example.com")
                .password("password123")
                .roles(Arrays.asList("USER"))
                .build();

        userDto = UserDto.builder()
                .id("testuser")
                .name("Test User")
                .nickname("tester")
                .email("test@example.com")
                .roles(Arrays.asList("USER"))
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .build();

        loginDto = LoginDto.builder()
                .userId("testuser")
                .password("password123")
                .build();

        tokenDto = TokenDto.builder()
                .token("jwt.token.here")
                .expiresAt(Instant.now().plusSeconds(7200).getEpochSecond())
                .issuedAt(Instant.now().getEpochSecond())
                .build();

        changePasswordDto = ChangePasswordDto.builder()
                .oldPassword("oldpassword")
                .newPassword("newpassword")
                .build();
    }

    @Test
    void createUser_Success() throws Exception {
        when(identityService.createUser(any(CreateUserDto.class))).thenReturn(userDto);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(identityService).createUser(any(CreateUserDto.class));
    }

    @Test
    void createUser_UserAlreadyExists() throws Exception {
        when(identityService.createUser(any(CreateUserDto.class)))
                .thenThrow(new IllegalArgumentException("User already exists"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isBadRequest());

        verify(identityService).createUser(any(CreateUserDto.class));
    }

    @Test
    void getUser_Success_OwnData() throws Exception {
        when(identityService.getUser("testuser")).thenReturn(userDto);

        mockMvc.perform(get("/users/testuser")
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("Test User"));

        verify(identityService).getUser("testuser");
    }

    @Test
    void getUser_Success_AdminAccess() throws Exception {
        when(identityService.getUser("otheruserid")).thenReturn(userDto);

        mockMvc.perform(get("/users/otheruserid")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"));

        verify(identityService).getUser("otheruserid");
    }

    @Test
    void getUser_Forbidden_NotOwnDataAndNotAdmin() throws Exception {
        mockMvc.perform(get("/users/otheruserid")
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        verify(identityService, never()).getUser(anyString());
    }

    @Test
    void getUser_NotFound() throws Exception {
        when(identityService.getUser("nonexistent"))
                .thenThrow(new IllegalArgumentException("User not found"));

        mockMvc.perform(get("/users/nonexistent")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isNotFound());

        verify(identityService).getUser("nonexistent");
    }

    @Test
    void updateUser_Success() throws Exception {
        when(identityService.updateUser(eq("testuser"), any(UserDto.class))).thenReturn(userDto);

        mockMvc.perform(put("/users/testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"));

        verify(identityService).updateUser(eq("testuser"), any(UserDto.class));
    }

    @Test
    void updateUser_Forbidden() throws Exception {
        mockMvc.perform(put("/users/otheruserid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        verify(identityService, never()).updateUser(anyString(), any(UserDto.class));
    }

    @Test
    void deleteUser_Success_Admin() throws Exception {
        doNothing().when(identityService).deleteUser("testuser");

        mockMvc.perform(delete("/users/testuser")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isNoContent());

        verify(identityService).deleteUser("testuser");
    }

    @Test
    void deleteUser_Forbidden_NotAdmin() throws Exception {
        mockMvc.perform(delete("/users/testuser")
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        verify(identityService, never()).deleteUser(anyString());
    }

    @Test
    void getAllUsers_Success_Admin() throws Exception {
        List<UserDto> users = Arrays.asList(userDto);
        when(identityService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/users")
                        .requestAttr("userId", "admin")
                        .requestAttr("userRoles", Arrays.asList("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("testuser"));

        verify(identityService).getAllUsers();
    }

    @Test
    void getAllUsers_Forbidden_NotAdmin() throws Exception {
        mockMvc.perform(get("/users")
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        verify(identityService, never()).getAllUsers();
    }

    @Test
    void login_Success() throws Exception {
        when(identityService.login(any(LoginDto.class))).thenReturn(tokenDto);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.issuedAt").exists());

        verify(identityService).login(any(LoginDto.class));
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        when(identityService.login(any(LoginDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());

        verify(identityService).login(any(LoginDto.class));
    }

    @Test
    void renewToken_Success() throws Exception {
        when(identityService.renewToken("old.token")).thenReturn(tokenDto);

        mockMvc.perform(post("/token/renew")
                        .header("Authorization", "Bearer old.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));

        verify(identityService).renewToken("old.token");
    }

    @Test
    void renewToken_MissingAuthHeader() throws Exception {
        mockMvc.perform(post("/token/renew"))
                .andExpect(status().isBadRequest());

        verify(identityService, never()).renewToken(anyString());
    }

    @Test
    void changePassword_Success() throws Exception {
        doNothing().when(identityService).changePassword(eq("testuser"), any(ChangePasswordDto.class));

        mockMvc.perform(post("/users/testuser/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isOk());

        verify(identityService).changePassword(eq("testuser"), any(ChangePasswordDto.class));
    }

    @Test
    void changePassword_Forbidden() throws Exception {
        mockMvc.perform(post("/users/otheruserid/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto))
                        .requestAttr("userId", "testuser")
                        .requestAttr("userRoles", Arrays.asList("USER")))
                .andExpect(status().isForbidden());

        verify(identityService, never()).changePassword(anyString(), any(ChangePasswordDto.class));
    }

    @Test
    void health_Success() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Identity Service is running"));
    }
}
