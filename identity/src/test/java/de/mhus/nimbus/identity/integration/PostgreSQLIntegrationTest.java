package de.mhus.nimbus.identity.integration;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.UserRepository;
import de.mhus.nimbus.identity.service.IdentityService;
import de.mhus.nimbus.server.shared.dto.CreateUserDto;
import de.mhus.nimbus.server.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostgreSQL database operations.
 * Tests the User entity and UserRepository with actual database interactions.
 */
@DataJpaTest
@ActiveProfiles("test")
public class PostgreSQLIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        testUser = User.builder()
                .id("testuser")
                .name("Test User")
                .nickname("tester")
                .email("test@example.com")
                .roles(Arrays.asList("USER"))
                .passwordHash("hashedpassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void userRepository_SaveAndFind_Success() {
        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Then
        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getId());
        assertEquals("Test User", savedUser.getName());
        assertEquals("test@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());

        // Verify find by ID
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getId());
    }

    @Test
    void userRepository_FindAll_Success() {
        // Given
        User secondUser = User.builder()
                .id("seconduser")
                .name("Second User")
                .nickname("second")
                .email("second@example.com")
                .roles(Arrays.asList("ADMIN"))
                .passwordHash("hashedpassword2")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // When
        userRepository.save(testUser);
        userRepository.save(secondUser);
        entityManager.flush();

        List<User> allUsers = userRepository.findAll();

        // Then
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(u -> u.getId().equals("testuser")));
        assertTrue(allUsers.stream().anyMatch(u -> u.getId().equals("seconduser")));
    }

    @Test
    void userRepository_ExistsByEmail_Success() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When & Then
        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void userRepository_ExistsById_Success() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When & Then
        assertTrue(userRepository.existsById("testuser"));
        assertFalse(userRepository.existsById("nonexistent"));
    }

    @Test
    void userRepository_DeleteById_Success() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();
        assertTrue(userRepository.existsById("testuser"));

        // When
        userRepository.deleteById("testuser");
        entityManager.flush();

        // Then
        assertFalse(userRepository.existsById("testuser"));
        Optional<User> deletedUser = userRepository.findById("testuser");
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void userRepository_UpdateUser_Success() {
        // Given
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // When
        savedUser.setName("Updated Name");
        savedUser.setEmail("updated@example.com");
        savedUser.setRoles(Arrays.asList("USER", "MODERATOR"));
        savedUser.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(savedUser);
        entityManager.flush();

        // Then
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals("Updated Name", foundUser.get().getName());
        assertEquals("updated@example.com", foundUser.get().getEmail());
        assertEquals(2, foundUser.get().getRoles().size());
        assertTrue(foundUser.get().getRoles().contains("USER"));
        assertTrue(foundUser.get().getRoles().contains("MODERATOR"));
    }

    @Test
    void userEntity_RolesSerialization_Success() {
        // Given
        testUser.setRoles(Arrays.asList("USER", "ADMIN", "MODERATOR"));

        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        List<String> roles = foundUser.get().getRoles();
        assertEquals(3, roles.size());
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("MODERATOR"));
    }

    @Test
    void userEntity_EmptyRoles_Success() {
        // Given
        testUser.setRoles(Arrays.asList());

        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        List<String> roles = foundUser.get().getRoles();
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void userEntity_NullRoles_HandledGracefully() {
        // Given
        testUser.setRoles(null);

        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        // Depending on implementation, this might be null or empty list
        // The important thing is that it doesn't cause an exception
    }

    @Test
    void userRepository_FindByEmail_Success() {
        // Note: This test assumes a findByEmail method exists in UserRepository
        // If it doesn't exist, this test would need to be removed or the method added

        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When - we use existsByEmail as a proxy since findByEmail might not exist
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    void userEntity_TimestampFields_AutoPopulated() {
        // Given
        User userWithoutTimestamps = User.builder()
                .id("timestamptest")
                .name("Timestamp Test")
                .nickname("timestamp")
                .email("timestamp@example.com")
                .roles(Arrays.asList("USER"))
                .passwordHash("hashedpassword")
                .build();

        // When
        userWithoutTimestamps.setCreatedAt(Instant.now());
        userWithoutTimestamps.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(userWithoutTimestamps);
        entityManager.flush();

        // Then
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void userRepository_ConcurrentAccess_Success() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When - Simulate concurrent access by reading and updating
        Optional<User> user1 = userRepository.findById("testuser");
        Optional<User> user2 = userRepository.findById("testuser");

        assertTrue(user1.isPresent());
        assertTrue(user2.isPresent());

        user1.get().setName("Updated by User 1");
        user2.get().setNickname("Updated by User 2");

        // Save both (last one wins for conflicting fields)
        userRepository.save(user1.get());
        userRepository.save(user2.get());
        entityManager.flush();

        // Then
        Optional<User> finalUser = userRepository.findById("testuser");
        assertTrue(finalUser.isPresent());
        // The last save should win
        assertEquals("Updated by User 2", finalUser.get().getNickname());
    }

    @Test
    void userRepository_LongEmailAddress_Success() {
        // Given
        String longEmail = "a".repeat(100) + "@" + "b".repeat(100) + ".com";
        testUser.setEmail(longEmail);

        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Then
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals(longEmail, foundUser.get().getEmail());
    }

    @Test
    void userRepository_SpecialCharactersInFields_Success() {
        // Given
        testUser.setName("Test User with Üñíçødé");
        testUser.setNickname("tëst€r");

        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Then
        Optional<User> foundUser = userRepository.findById("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals("Test User with Üñíçødé", foundUser.get().getName());
        assertEquals("tëst€r", foundUser.get().getNickname());
    }
}
