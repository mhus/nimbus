package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import de.mhus.nimbus.identity.util.JwtTokenUtils;
import de.mhus.nimbus.server.shared.dto.*;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityService.
 * Tests all business logic operations for user management and authentication.
 */
@ExtendWith(MockitoExtension.class)
public class IdentityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdentityServiceUtils identityServiceUtils;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private IdentityService identityService;

    private CreateUserDto createUserDto;
    private User user;
    private UserDto userDto;
    private LoginDto loginDto;
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

        user = User.builder()
                .id("testuser")
                .name("Test User")
                .nickname("tester")
                .email("test@example.com")
                .roles(Arrays.asList("USER"))
                .passwordHash("hashedpassword")
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
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

        changePasswordDto = ChangePasswordDto.builder()
                .oldPassword("oldpassword")
                .newPassword("newpassword")
                .build();
    }

    @Test
    void createUser_Success() {
        // Given
        when(userRepository.existsById("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(identityServiceUtils.hashPassword("password123", "testuser")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = identityService.createUser(createUserDto);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).existsById("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(identityServiceUtils).hashPassword("password123", "testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_UserIdAlreadyExists() {
        // Given
        when(userRepository.existsById("testuser")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.createUser(createUserDto)
        );

        assertEquals("User with ID testuser already exists", exception.getMessage());
        verify(userRepository).existsById("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists() {
        // Given
        when(userRepository.existsById("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.createUser(createUserDto)
        );

        assertEquals("User with email test@example.com already exists", exception.getMessage());
        verify(userRepository).existsById("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUser_Success() {
        // Given
        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));

        // When
        UserDto result = identityService.getUser("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getId());
        assertEquals("Test User", result.getName());
        verify(userRepository).findById("testuser");
    }

    @Test
    void getUser_NotFound() {
        // Given
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.getUser("nonexistent")
        );

        assertEquals("User not found: nonexistent", exception.getMessage());
        verify(userRepository).findById("nonexistent");
    }

    @Test
    void updateUser_Success() {
        // Given
        UserDto updateDto = UserDto.builder()
                .id("testuser")
                .name("Updated Name")
                .nickname("updated")
                .email("updated@example.com")
                .roles(Arrays.asList("USER", "MODERATOR"))
                .build();

        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = identityService.updateUser("testuser", updateDto);

        // Then
        assertNotNull(result);
        verify(userRepository).findById("testuser");
        verify(userRepository).existsByEmail("updated@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_EmailAlreadyExists() {
        // Given
        UserDto updateDto = UserDto.builder()
                .id("testuser")
                .name("Updated Name")
                .nickname("updated")
                .email("existing@example.com")
                .roles(Arrays.asList("USER"))
                .build();

        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.updateUser("testuser", updateDto)
        );

        assertEquals("Email existing@example.com already exists", exception.getMessage());
        verify(userRepository).findById("testuser");
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        // Given
        when(userRepository.existsById("testuser")).thenReturn(true);

        // When
        identityService.deleteUser("testuser");

        // Then
        verify(userRepository).existsById("testuser");
        verify(userRepository).deleteById("testuser");
    }

    @Test
    void deleteUser_NotFound() {
        // Given
        when(userRepository.existsById("nonexistent")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.deleteUser("nonexistent")
        );

        assertEquals("User not found: nonexistent", exception.getMessage());
        verify(userRepository).existsById("nonexistent");
        verify(userRepository, never()).deleteById(anyString());
    }

    @Test
    void getAllUsers_Success() {
        // Given
        List<User> users = Arrays.asList(user);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto> result = identityService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getId());
        verify(userRepository).findAll();
    }

    @Test
    void login_Success() {
        // Given
        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));
        when(identityServiceUtils.verifyPassword("password123", "testuser", "hashedpassword")).thenReturn(true);
        when(jwtTokenUtils.createToken("testuser", Arrays.asList("USER"))).thenReturn("jwt.token.here");
        when(jwtTokenUtils.getExpirationTime("jwt.token.here")).thenReturn(System.currentTimeMillis() / 1000 + 7200);
        when(jwtTokenUtils.getIssuedTime("jwt.token.here")).thenReturn(System.currentTimeMillis() / 1000);

        // When
        TokenDto result = identityService.login(loginDto);

        // Then
        assertNotNull(result);
        assertEquals("jwt.token.here", result.getToken());
        assertNotNull(result.getExpiresAt());
        assertNotNull(result.getIssuedAt());

        verify(userRepository).findById("testuser");
        verify(identityServiceUtils).verifyPassword("password123", "testuser", "hashedpassword");
        verify(jwtTokenUtils).createToken("testuser", Arrays.asList("USER"));
    }

    @Test
    void login_UserNotFound() {
        // Given
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        LoginDto invalidLoginDto = LoginDto.builder()
                .userId("nonexistent")
                .password("password123")
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.login(invalidLoginDto)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findById("nonexistent");
        verify(identityServiceUtils, never()).verifyPassword(anyString(), anyString(), anyString());
    }

    @Test
    void login_InvalidPassword() {
        // Given
        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));
        when(identityServiceUtils.verifyPassword("wrongpassword", "testuser", "hashedpassword")).thenReturn(false);

        LoginDto invalidLoginDto = LoginDto.builder()
                .userId("testuser")
                .password("wrongpassword")
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.login(invalidLoginDto)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findById("testuser");
        verify(identityServiceUtils).verifyPassword("wrongpassword", "testuser", "hashedpassword");
        verify(jwtTokenUtils, never()).createToken(anyString(), anyList());
    }

    @Test
    void renewToken_Success() {
        // Given
        when(identityServiceUtils.extractUserId("old.token")).thenReturn("testuser");
        when(identityServiceUtils.extractRoles("old.token")).thenReturn(Arrays.asList("USER"));
        when(userRepository.existsById("testuser")).thenReturn(true);
        when(jwtTokenUtils.createToken("testuser", Arrays.asList("USER"))).thenReturn("new.token.here");
        when(jwtTokenUtils.getExpirationTime("new.token.here")).thenReturn(System.currentTimeMillis() / 1000 + 7200);
        when(jwtTokenUtils.getIssuedTime("new.token.here")).thenReturn(System.currentTimeMillis() / 1000);

        // When
        TokenDto result = identityService.renewToken("old.token");

        // Then
        assertNotNull(result);
        assertEquals("new.token.here", result.getToken());

        verify(identityServiceUtils).extractUserId("old.token");
        verify(identityServiceUtils).extractRoles("old.token");
        verify(userRepository).existsById("testuser");
        verify(jwtTokenUtils).createToken("testuser", Arrays.asList("USER"));
    }

    @Test
    void renewToken_InvalidToken() {
        // Given
        when(identityServiceUtils.extractUserId("invalid.token")).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.renewToken("invalid.token")
        );

        assertEquals("Invalid token", exception.getMessage());
        verify(identityServiceUtils).extractUserId("invalid.token");
        verify(jwtTokenUtils, never()).createToken(anyString(), anyList());
    }

    @Test
    void renewToken_UserNoLongerExists() {
        // Given
        when(identityServiceUtils.extractUserId("token")).thenReturn("deleteduser");
        when(identityServiceUtils.extractRoles("token")).thenReturn(Arrays.asList("USER"));
        when(userRepository.existsById("deleteduser")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.renewToken("token")
        );

        assertEquals("User no longer exists", exception.getMessage());
        verify(userRepository).existsById("deleteduser");
        verify(jwtTokenUtils, never()).createToken(anyString(), anyList());
    }

    @Test
    void changePassword_Success() {
        // Given
        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));
        when(identityServiceUtils.verifyPassword("oldpassword", "testuser", "hashedpassword")).thenReturn(true);
        when(identityServiceUtils.hashPassword("newpassword", "testuser")).thenReturn("newhashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        identityService.changePassword("testuser", changePasswordDto);

        // Then
        verify(userRepository).findById("testuser");
        verify(identityServiceUtils).verifyPassword("oldpassword", "testuser", "hashedpassword");
        verify(identityServiceUtils).hashPassword("newpassword", "testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_UserNotFound() {
        // Given
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.changePassword("nonexistent", changePasswordDto)
        );

        assertEquals("User not found: nonexistent", exception.getMessage());
        verify(userRepository).findById("nonexistent");
        verify(identityServiceUtils, never()).verifyPassword(anyString(), anyString(), anyString());
    }

    @Test
    void changePassword_InvalidOldPassword() {
        // Given
        when(userRepository.findById("testuser")).thenReturn(Optional.of(user));
        when(identityServiceUtils.verifyPassword("wrongoldpassword", "testuser", "hashedpassword")).thenReturn(false);

        ChangePasswordDto invalidDto = ChangePasswordDto.builder()
                .oldPassword("wrongoldpassword")
                .newPassword("newpassword")
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.changePassword("testuser", invalidDto)
        );

        assertEquals("Invalid old password", exception.getMessage());
        verify(userRepository).findById("testuser");
        verify(identityServiceUtils).verifyPassword("wrongoldpassword", "testuser", "hashedpassword");
        verify(userRepository, never()).save(any(User.class));
    }
}
