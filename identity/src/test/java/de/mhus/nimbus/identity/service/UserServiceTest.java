package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für UserService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateUser() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        String firstName = "Test";
        String lastName = "User";

        // When
        User createdUser = userService.createUser(username, email, password, firstName, lastName);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals(username, createdUser.getUsername());
        assertEquals(email, createdUser.getEmail());
        assertEquals(firstName, createdUser.getFirstName());
        assertEquals(lastName, createdUser.getLastName());
        assertTrue(createdUser.getActive());
        assertNotNull(createdUser.getCreatedAt());
    }

    @Test
    void testCreateUserWithExistingUsername() {
        // Given
        String username = "testuser";
        String email1 = "test1@example.com";
        String email2 = "test2@example.com";

        userService.createUser(username, email1, "password123", "Test", "User");

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            userService.createUser(username, email2, "password123", "Test", "User2"));
    }

    @Test
    void testFindByUsername() {
        // Given
        String username = "finduser";
        User createdUser = userService.createUser(username, "find@example.com", "password123", "Find", "User");

        // When
        User foundUser = userService.findByUsername(username).orElse(null);

        // Then
        assertNotNull(foundUser);
        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals(username, foundUser.getUsername());
    }
}
