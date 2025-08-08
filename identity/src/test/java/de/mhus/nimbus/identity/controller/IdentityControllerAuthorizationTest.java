package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.service.IdentityService;
import de.mhus.nimbus.server.shared.dto.UserDto;
import de.mhus.nimbus.server.shared.util.AuthorizationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityController with AuthorizationUtils integration.
 */
class IdentityControllerAuthorizationTest {

    @Mock
    private IdentityService identityService;

    private IdentityController identityController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        identityController = new IdentityController(identityService);
    }

    @Test
    @DisplayName("getUser should allow user to access their own data")
    void getUserShouldAllowUserToAccessOwnData() {
        // Given
        String userId = "testUser";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("userRoles", Arrays.asList("USER"));

        UserDto expectedUser = UserDto.builder()
                .id(userId)
                .name("Test User")
                .roles(Arrays.asList("USER"))
                .build();

        when(identityService.getUser(userId)).thenReturn(expectedUser);

        // When
        ResponseEntity<UserDto> response = identityController.getUser(userId, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedUser, response.getBody());
        verify(identityService).getUser(userId);
    }

    @Test
    @DisplayName("getUser should allow admin to access any user data")
    void getUserShouldAllowAdminToAccessAnyUserData() {
        // Given
        String adminId = "admin";
        String targetUserId = "otherUser";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", adminId);
        request.setAttribute("userRoles", Arrays.asList(AuthorizationUtils.Roles.ADMIN));

        UserDto expectedUser = UserDto.builder()
                .id(targetUserId)
                .name("Other User")
                .roles(Arrays.asList("USER"))
                .build();

        when(identityService.getUser(targetUserId)).thenReturn(expectedUser);

        // When
        ResponseEntity<UserDto> response = identityController.getUser(targetUserId, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedUser, response.getBody());
        verify(identityService).getUser(targetUserId);
    }

    @Test
    @DisplayName("getUser should deny access when user tries to access other user's data")
    void getUserShouldDenyAccessWhenUserTriesToAccessOtherUserData() {
        // Given
        String userId = "user1";
        String targetUserId = "user2";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("userRoles", Arrays.asList("USER"));

        // When
        ResponseEntity<UserDto> response = identityController.getUser(targetUserId, request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(identityService, never()).getUser(anyString());
    }

    @Test
    @DisplayName("deleteUser should allow only admin access")
    void deleteUserShouldAllowOnlyAdminAccess() {
        // Given
        String adminId = "admin";
        String targetUserId = "userToDelete";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", adminId);
        request.setAttribute("userRoles", Arrays.asList(AuthorizationUtils.Roles.ADMIN));

        // When
        ResponseEntity<Void> response = identityController.deleteUser(targetUserId, request);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(identityService).deleteUser(targetUserId);
    }

    @Test
    @DisplayName("deleteUser should deny access for non-admin users")
    void deleteUserShouldDenyAccessForNonAdminUsers() {
        // Given
        String userId = "regularUser";
        String targetUserId = "userToDelete";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("userRoles", Arrays.asList("USER"));

        // When
        ResponseEntity<Void> response = identityController.deleteUser(targetUserId, request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(identityService, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("getAllUsers should allow only admin access")
    void getAllUsersShouldAllowOnlyAdminAccess() {
        // Given
        String adminId = "admin";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", adminId);
        request.setAttribute("userRoles", Arrays.asList(AuthorizationUtils.Roles.ADMIN));

        List<UserDto> expectedUsers = Arrays.asList(
                UserDto.builder().id("user1").name("User 1").build(),
                UserDto.builder().id("user2").name("User 2").build()
        );

        when(identityService.getAllUsers()).thenReturn(expectedUsers);

        // When
        ResponseEntity<List<UserDto>> response = identityController.getAllUsers(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedUsers, response.getBody());
        verify(identityService).getAllUsers();
    }

    @Test
    @DisplayName("getAllUsers should deny access for non-admin users")
    void getAllUsersShouldDenyAccessForNonAdminUsers() {
        // Given
        String userId = "regularUser";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("userRoles", Arrays.asList("USER"));

        // When
        ResponseEntity<List<UserDto>> response = identityController.getAllUsers(request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(identityService, never()).getAllUsers();
    }

    @Test
    @DisplayName("updateUser should allow user to update their own data")
    void updateUserShouldAllowUserToUpdateOwnData() {
        // Given
        String userId = "testUser";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("userRoles", Arrays.asList("USER"));

        UserDto userDto = UserDto.builder()
                .id(userId)
                .name("Updated Name")
                .roles(Arrays.asList("USER"))
                .build();

        when(identityService.updateUser(userId, userDto)).thenReturn(userDto);

        // When
        ResponseEntity<UserDto> response = identityController.updateUser(userId, userDto, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDto, response.getBody());
        verify(identityService).updateUser(userId, userDto);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when no user ID in request")
    void shouldReturnUnauthorizedWhenNoUserIdInRequest() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No userId attribute set

        // When
        ResponseEntity<UserDto> response = identityController.getUser("someUser", request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(identityService, never()).getUser(anyString());
    }

    @Test
    @DisplayName("Should handle case-insensitive role checking through AuthorizationUtils")
    void shouldHandleCaseInsensitiveRoleCheckingThroughAuthorizationUtils() {
        // Given
        String adminId = "admin";
        String targetUserId = "userToDelete";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", adminId);
        request.setAttribute("userRoles", Arrays.asList("admin")); // lowercase

        // When
        ResponseEntity<Void> response = identityController.deleteUser(targetUserId, request);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(identityService).deleteUser(targetUserId);
    }
}
