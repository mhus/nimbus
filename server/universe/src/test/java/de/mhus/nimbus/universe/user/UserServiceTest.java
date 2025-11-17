package de.mhus.nimbus.universe.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

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
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // reflectively set id if needed (or mimic persistence)
            // For simplicity we just return as-is; id stays null in pure unit test
            return u;
        });

        User u = userService.createUser("alpha","alpha@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
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
        User stored = new User("alpha","alpha@example.com");
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
        User a = new User("alpha","alpha@example.com");
        User b = new User("beta","beta@example.com");
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
}
