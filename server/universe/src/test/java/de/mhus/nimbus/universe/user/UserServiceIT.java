package de.mhus.nimbus.universe.user;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserServiceIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_ok() {
        User u = userService.createUser("alpha","alpha@example.com");
        assertNotNull(u.getId());
        assertEquals("alpha", u.getUsername());
        assertEquals("alpha@example.com", u.getEmail());
        assertNotNull(u.getCreatedAt());
    }

    @Test
    void createUser_duplicateUsername() {
        userService.createUser("alpha","a@example.com");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser("alpha","b@example.com"));
        assertTrue(ex.getMessage().contains("Username"));
    }

    @Test
    void createUser_duplicateEmail() {
        userService.createUser("alpha","same@example.com");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser("beta","same@example.com"));
        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    void getByUsername_found() {
        userService.createUser("alpha","alpha@example.com");
        assertTrue(userService.getByUsername("alpha").isPresent());
    }

    @Test
    void getById_notFound() {
        assertFalse(userService.getById("nonexistent-id").isPresent());
    }

    @Test
    void listAll_and_delete() {
        User a = userService.createUser("alpha","alpha@example.com");
        User b = userService.createUser("beta","beta@example.com");
        List<User> list = userService.listAll();
        assertEquals(2, list.size());
        userService.deleteById(a.getId());
        assertEquals(1, userService.listAll().size());
        assertFalse(userService.getById(a.getId()).isPresent());
        assertTrue(userService.getById(b.getId()).isPresent());
    }
}
