package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.security.USecurityProperties;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.user.UniverseRoles;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginControllerUniverseClaimTest {

    @Test
    void login_includes_universe_claim_when_roles_present() {
        UUserService userService = mock(UUserService.class);
        JwtService jwtService = mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        props.setAuthKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");
        props.setExpiresMinutes(60);

        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1");
        user.setRoles(UniverseRoles.USER, UniverseRoles.ADMIN);
        when(userService.getByUsername("alpha")).thenReturn(Optional.of(user));
        when(userService.validatePassword("u1","pw")).thenReturn(true);
        when(jwtService.createTokenWithSecretKey(eq(props.getAuthKeyId()), eq("u1"), any(Map.class), any(Instant.class))).thenAnswer(inv -> {
            Map<?,?> claims = inv.getArgument(2);
            assertTrue(claims.containsKey("universe"));
            assertEquals("USER,ADMIN", claims.get("universe"));
            return "jwt-token";
        });

        ULoginController controller = new ULoginController(userService, jwtService, props);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("alpha","pw"));
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void login_omits_universe_claim_when_no_roles() {
        UUserService userService = mock(UUserService.class);
        JwtService jwtService = mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        props.setAuthKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");
        props.setExpiresMinutes(60);

        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1"); // keine Rollen gesetzt
        when(userService.getByUsername("alpha")).thenReturn(Optional.of(user));
        when(userService.validatePassword("u1","pw")).thenReturn(true);
        when(jwtService.createTokenWithSecretKey(eq(props.getAuthKeyId()), eq("u1"), any(Map.class), any(Instant.class))).thenAnswer(inv -> {
            Map<?,?> claims = inv.getArgument(2);
            assertFalse(claims.containsKey("universe"));
            return "jwt-token";
        });

        ULoginController controller = new ULoginController(userService, jwtService, props);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("alpha","pw"));
        assertEquals(200, resp.getStatusCode().value());
    }
}

