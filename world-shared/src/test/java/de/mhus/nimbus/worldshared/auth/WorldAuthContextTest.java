package de.mhus.nimbus.worldshared.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldAuthContextTest {

    @Test
    void testAuthenticatedContext() {
        List<String> roles = List.of("ADMIN", "USER", "MODERATOR");
        WorldAuthContext context = WorldAuthContext.builder()
            .username("testuser")
            .roles(roles)
            .authenticated(true)
            .build();

        assertTrue(context.isAuthenticated());
        assertEquals("testuser", context.getUsername());
        assertEquals(roles, context.getRoles());
    }

    @Test
    void testUnauthenticatedContext() {
        WorldAuthContext context = WorldAuthContext.builder()
            .authenticated(false)
            .build();

        assertFalse(context.isAuthenticated());
        assertNull(context.getUsername());
        assertNull(context.getRoles());
    }

    @Test
    void testHasRole_UserHasRole() {
        WorldAuthContext context = WorldAuthContext.builder()
            .username("testuser")
            .roles(List.of("ADMIN", "USER"))
            .authenticated(true)
            .build();

        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasRole("USER"));
        assertFalse(context.hasRole("MODERATOR"));
    }

    @Test
    void testHasRole_UnauthenticatedUser() {
        WorldAuthContext context = WorldAuthContext.builder()
            .authenticated(false)
            .build();

        assertFalse(context.hasRole("ADMIN"));
        assertFalse(context.hasRole("USER"));
    }

    @Test
    void testHasRole_NoRoles() {
        WorldAuthContext context = WorldAuthContext.builder()
            .username("testuser")
            .roles(null)
            .authenticated(true)
            .build();

        assertFalse(context.hasRole("ADMIN"));
    }

    @Test
    void testHasAnyRole_UserHasOneRole() {
        WorldAuthContext context = WorldAuthContext.builder()
            .username("testuser")
            .roles(List.of("USER"))
            .authenticated(true)
            .build();

        assertTrue(context.hasAnyRole("ADMIN", "USER", "MODERATOR"));
        assertTrue(context.hasAnyRole("USER"));
        assertFalse(context.hasAnyRole("ADMIN", "MODERATOR"));
    }

    @Test
    void testHasAnyRole_UnauthenticatedUser() {
        WorldAuthContext context = WorldAuthContext.builder()
            .authenticated(false)
            .build();

        assertFalse(context.hasAnyRole("ADMIN", "USER"));
    }

    @Test
    void testHasAnyRole_NoRoles() {
        WorldAuthContext context = WorldAuthContext.builder()
            .username("testuser")
            .roles(null)
            .authenticated(true)
            .build();

        assertFalse(context.hasAnyRole("ADMIN", "USER"));
    }

    @Test
    void testHasAnyRole_EmptyRolesList() {
        WorldAuthContext context = WorldAuthContext.builder()
            .username("testuser")
            .roles(List.of())
            .authenticated(true)
            .build();

        assertFalse(context.hasAnyRole("ADMIN", "USER"));
    }
}
