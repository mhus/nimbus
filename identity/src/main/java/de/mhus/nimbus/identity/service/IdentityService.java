package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import de.mhus.nimbus.identity.util.JwtTokenUtils;
import de.mhus.nimbus.server.shared.dto.*;
import de.mhus.nimbus.server.shared.util.AuthorizationUtils;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for identity and user management operations.
 * Implements all functionality for user CRUD operations, authentication and JWT token management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IdentityService {

    private final UserRepository userRepository;
    private final IdentityServiceUtils identityServiceUtils;
    private final JwtTokenUtils jwtTokenUtils;

    /**
     * Creates a new user with the provided information.
     * @param createUserDto user creation data
     * @return created user information
     * @throws IllegalArgumentException if user ID or email already exists
     */
    public UserDto createUser(CreateUserDto createUserDto) {
        log.info("Creating new user with ID: {}", createUserDto.getId());

        // Check if user ID already exists
        if (userRepository.existsById(createUserDto.getId())) {
            throw new IllegalArgumentException("User with ID " + createUserDto.getId() + " already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new IllegalArgumentException("User with email " + createUserDto.getEmail() + " already exists");
        }

        // Hash the password
        String passwordHash = identityServiceUtils.hashPassword(createUserDto.getPassword(), createUserDto.getId());

        // Create user entity
        User user = User.builder()
                .id(createUserDto.getId())
                .name(createUserDto.getName())
                .nickname(createUserDto.getNickname())
                .email(createUserDto.getEmail())
                .roles(createUserDto.getRoles())
                .passwordHash(passwordHash)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getId());

        return convertToDto(savedUser);
    }

    /**
     * Retrieves user information by ID.
     * @param userId the user ID
     * @return user information
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public UserDto getUser(String userId) {
        log.debug("Retrieving user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return convertToDto(user);
    }

    /**
     * Updates user information.
     * @param userId the user ID
     * @param userDto updated user data
     * @return updated user information
     * @throws IllegalArgumentException if user not found or email already exists
     */
    public UserDto updateUser(String userId, UserDto userDto) {
        log.info("Updating user: {}", userId);

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if email is being changed and if new email already exists
        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
            userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email " + userDto.getEmail() + " already exists");
        }

        // Update user fields
        existingUser.setName(userDto.getName());
        existingUser.setNickname(userDto.getNickname());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setRoles(userDto.getRoles());

        User savedUser = userRepository.save(existingUser);
        log.info("User updated successfully: {}", savedUser.getId());

        return convertToDto(savedUser);
    }

    /**
     * Deletes a user by ID.
     * @param userId the user ID
     * @throws IllegalArgumentException if user not found
     */
    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("User deleted successfully: {}", userId);
    }

    /**
     * Retrieves all users.
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.debug("Retrieving all users");

        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Authenticates a user and generates a JWT token.
     * @param loginDto login credentials
     * @return JWT token information
     * @throws IllegalArgumentException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public TokenDto login(LoginDto loginDto) {
        log.info("Login attempt for user: {}", loginDto.getUserId());

        User user = userRepository.findById(loginDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Verify password
        if (!identityServiceUtils.verifyPassword(loginDto.getPassword(), user.getId(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", loginDto.getUserId());
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Generate JWT token using RSA private key
        String token = jwtTokenUtils.createToken(user.getId(), calculateUserRoles(user));
        Long expiresAt = jwtTokenUtils.getExpirationTime(token);
        Long issuedAt = jwtTokenUtils.getIssuedTime(token);

        log.info("Login successful for user: {}", loginDto.getUserId());

        return TokenDto.builder()
                .token(token)
                .expiresAt(expiresAt)
                .issuedAt(issuedAt)
                .build();
    }

    /**
     * Renews an existing JWT token.
     * @param currentToken the current JWT token
     * @return new JWT token information
     * @throws IllegalArgumentException if token is invalid
     */
    @Transactional(readOnly = true)
    public TokenDto renewToken(String currentToken) {
        log.debug("Renewing token");

        String userId = identityServiceUtils.extractUserId(currentToken);
        List<String> roles = identityServiceUtils.extractRoles(currentToken);

        if (userId == null || roles == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        // Verify user still exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User no longer exists");
        }

        // Generate new token using RSA private key
        String newToken = jwtTokenUtils.createToken(userId, roles);
        Long expiresAt = jwtTokenUtils.getExpirationTime(newToken);
        Long issuedAt = jwtTokenUtils.getIssuedTime(newToken);

        log.info("Token renewed for user: {}", userId);

        return TokenDto.builder()
                .token(newToken)
                .expiresAt(expiresAt)
                .issuedAt(issuedAt)
                .build();
    }

    /**
     * Changes user password.
     * @param userId the user ID
     * @param changePasswordDto password change data
     * @throws IllegalArgumentException if user not found or old password is incorrect
     */
    public void changePassword(String userId, ChangePasswordDto changePasswordDto) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Verify old password
        if (!identityServiceUtils.verifyPassword(changePasswordDto.getOldPassword(), user.getId(), user.getPasswordHash())) {
            log.warn("Invalid old password for user: {}", userId);
            throw new IllegalArgumentException("Invalid old password");
        }

        // Hash new password
        String newPasswordHash = identityServiceUtils.hashPassword(changePasswordDto.getNewPassword(), user.getId());
        user.setPasswordHash(newPasswordHash);

        userRepository.save(user);
        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Converts User entity to UserDto.
     * If the user has ADMIN role, all known roles are set.
     * @param user the user entity
     * @return user DTO
     */
    private UserDto convertToDto(User user) {
        List<String> roles = calculateUserRoles(user);

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private List<String> calculateUserRoles(final User user) {
        List<String> roles = user.getRoles();

        // If user has ADMIN role, set all known roles
        if (roles != null && roles.stream().anyMatch(role ->
                AuthorizationUtils.Roles.ADMIN.equalsIgnoreCase(role))) {
            log.debug("User {} has ADMIN role - setting all known roles", user.getId());
            roles = AuthorizationUtils.getAllKnownRoles();
        }
        return roles;
    }
}
