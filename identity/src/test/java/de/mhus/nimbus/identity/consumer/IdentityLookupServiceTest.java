package de.mhus.nimbus.identity.consumer;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.service.IdentityLookupService;
import de.mhus.nimbus.identity.service.UserService;
import de.mhus.nimbus.shared.avro.UserLookupRequest;
import de.mhus.nimbus.shared.avro.UserLookupResponse;
import de.mhus.nimbus.shared.avro.UserLookupStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für IdentityLookupService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityLookupServiceTest {

    @Autowired
    private IdentityLookupService identityLookupService;

    @Autowired
    private UserService userService;

    @Test
    void testProcessUserLookupRequestByUsername() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        User createdUser = userService.createUser(username, email, "password123", "Test", "User");

        UserLookupRequest request = UserLookupRequest.newBuilder()
                .setRequestId("test-request-1")
                .setUserId(null)
                .setUsername(username)
                .setEmail(null)
                .setTimestamp(Instant.now().toEpochMilli())
                .setRequestedBy("test-service")
                .build();

        // When
        UserLookupResponse response = identityLookupService.processUserLookupRequest(request);

        // Then
        assertNotNull(response);
        assertEquals("test-request-1", response.getRequestId());
        assertEquals(UserLookupStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getUser());
        assertEquals(createdUser.getId(), response.getUser().getId());
        assertEquals(username, response.getUser().getUsername());
        assertEquals(email, response.getUser().getEmail());
        assertNull(response.getErrorMessage());
    }

    @Test
    void testProcessUserLookupRequestUserNotFound() {
        // Given
        UserLookupRequest request = UserLookupRequest.newBuilder()
                .setRequestId("test-request-2")
                .setUserId(null)
                .setUsername("nonexistent")
                .setEmail(null)
                .setTimestamp(Instant.now().toEpochMilli())
                .setRequestedBy("test-service")
                .build();

        // When
        UserLookupResponse response = identityLookupService.processUserLookupRequest(request);

        // Then
        assertNotNull(response);
        assertEquals("test-request-2", response.getRequestId());
        assertEquals(UserLookupStatus.USER_NOT_FOUND, response.getStatus());
        assertNull(response.getUser());
        assertEquals("User not found", response.getErrorMessage());
    }

    @Test
    void testValidateUserLookupRequest() {
        // Valid request
        UserLookupRequest validRequest = UserLookupRequest.newBuilder()
                .setRequestId("valid-request")
                .setUsername("testuser")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        assertDoesNotThrow(() -> identityLookupService.validateUserLookupRequest(validRequest));

        // Invalid request - no search criteria
        UserLookupRequest invalidRequest = UserLookupRequest.newBuilder()
                .setRequestId("invalid-request")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        assertThrows(IllegalArgumentException.class,
                    () -> identityLookupService.validateUserLookupRequest(invalidRequest));
    }
}
