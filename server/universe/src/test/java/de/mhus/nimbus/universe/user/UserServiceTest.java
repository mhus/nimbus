package de.mhus.nimbus.universe.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.mhus.nimbus.shared.security.HashService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UUserRepository userRepository;

    @Mock
    private HashService hashService;

    @InjectMocks
    private UUserService userService;

    @BeforeEach
    void setup() {
        // reset mocks if needed
        clearInvocations(userRepository);
    }

    @Test
    void createUser_ok() {
        when(userRepository.existsByUsername("alpha")).thenReturn(false);
        when(userRepository.existsByEmail("alpha@example.com")).thenReturn(false);

        // Simulate save assigning an ID
        when(userRepository.save(any(UUser.class))).thenAnswer(invocation -> {
            UUser u = invocation.getArgument(0);
            // reflectively set id if needed (or mimic persistence)
            // For simplicity we just return as-is; id stays null in pure unit test
            return u;
        });

        UUser u = userService.createUser("alpha","alpha@example.com");

        ArgumentCaptor<UUser> captor = ArgumentCaptor.forClass(UUser.class);
        verify(userRepository).save(captor.capture());
        assertEquals("alpha", captor.getValue().getUsername());
        assertEquals("alpha@example.com", captor.getValue().getEmail());
        assertEquals("alpha", u.getUsername());
    }

    @Test
    void createUser_duplicateUsername() {
        when(userRepository.existsByUsername("alpha")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser("alpha","b@example.com"));
        assertTrue(ex.getMessage().contains("Username"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_duplicateEmail() {
        when(userRepository.existsByUsername("alpha")).thenReturn(false);
        when(userRepository.existsByEmail("same@example.com")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser("alpha","same@example.com"));
        assertTrue(ex.getMessage().contains("Email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getByUsername_found() {
        UUser stored = new UUser("alpha","alpha@example.com");
        when(userRepository.findByUsername("alpha")).thenReturn(Optional.of(stored));
        assertTrue(userService.getByUsername("alpha").isPresent());
    }

    @Test
    void getById_notFound() {
        when(userRepository.findById("nonexistent-id")).thenReturn(Optional.empty());
        assertFalse(userService.getById("nonexistent-id").isPresent());
    }

    @Test
    void listAll_and_delete() {
        UUser a = new UUser("alpha","alpha@example.com");
        UUser b = new UUser("beta","beta@example.com");
        a.setId("a");
        b.setId("b");

        // Initial stubs
        when(userRepository.findAll()).thenReturn(java.util.Arrays.asList(a,b));
        when(userRepository.findById("a")).thenReturn(java.util.Optional.of(a));
        when(userRepository.findById("b")).thenReturn(java.util.Optional.of(b));

        // First call should return both
        assertEquals(2, userService.listAll().size());

        // Simulate delete of 'a'
        doNothing().when(userRepository).deleteById("a");
        userService.deleteById("a");

        // Update stubs after deletion
        when(userRepository.findAll()).thenReturn(java.util.List.of(b));
        when(userRepository.findById("a")).thenReturn(java.util.Optional.empty());

        assertEquals(1, userService.listAll().size());
        assertFalse(userService.getById("a").isPresent());
        assertTrue(userService.getById("b").isPresent());
    }

    @Test
    void setPassword_ok() {
        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        // Salt muss Base64(userId) sein: "u1" -> "dTE="
        when(hashService.hash("secret", "u1")).thenReturn("SHA-256;dTE=;HASHED");
        when(userRepository.save(any(UUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UUser updated = userService.setPassword("u1", "secret");
        assertNotNull(updated.getPasswordHash());
        assertTrue(updated.getPasswordHash().startsWith("SHA-256;"));
        assertTrue(updated.getPasswordHash().contains("dTE=;"));
        verify(hashService).hash("secret", "u1");
    }

    @Test
    void validatePassword_ok() {
        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1");
        user.setPasswordHash("SHA-256;dTE=;HASHED");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(hashService.validate("secret", "u1", "SHA-256;dTE=;HASHED")).thenReturn(true);
        assertTrue(userService.validatePassword("u1", "secret"));
        verify(hashService).validate("secret", "u1", "SHA-256;dTE=;HASHED");
    }

    @Test
    void validatePassword_wrong() {
        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1");
        user.setPasswordHash("SHA-256;dTE=;HASHED");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(hashService.validate("bad", "u1", "SHA-256;dTE=;HASHED")).thenReturn(false);
        assertFalse(userService.validatePassword("u1", "bad"));
        verify(hashService).validate("bad", "u1", "SHA-256;dTE=;HASHED");
    }

    @Test
    void setPassword_blank() {
        assertThrows(IllegalArgumentException.class, () -> userService.setPassword("u1", " "));
    }
}
