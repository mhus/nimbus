package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.JwtProperties;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    @Test
    void login_success() {
        UUserService userService = mock(UUserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");
        props.setExpiresMinutes(60);

        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1");
        when(userService.getByUsername("alpha")).thenReturn(Optional.of(user));
        when(userService.validatePassword("u1","pw"))
                .thenReturn(true);
        when(jwtService.createTokenWithSecretKey(eq(props.getKeyId()), eq("u1"), any(Map.class), any(Instant.class)))
                .thenReturn("jwt-token");

        ULoginController controller = new ULoginController(userService, jwtService, props);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("alpha","pw"));
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("jwt-token", resp.getBody().token());
        assertEquals("u1", resp.getBody().userId());
    }

    @Test
    void login_wrong_password() {
        UUserService userService = mock(UUserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");

        UUser user = new UUser("alpha","alpha@example.com");
        user.setId("u1");
        when(userService.getByUsername("alpha")).thenReturn(Optional.of(user));
        when(userService.validatePassword("u1","bad"))
                .thenReturn(false);

        ULoginController controller = new ULoginController(userService, jwtService, props);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("alpha","bad"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void login_user_not_found() {
        UUserService userService = mock(UUserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");

        when(userService.getByUsername("alpha")).thenReturn(Optional.empty());
        ULoginController controller = new ULoginController(userService, jwtService, props);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("alpha","pw"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void login_bad_request() {
        UUserService userService = mock(UUserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");
        ULoginController controller = new ULoginController(userService, jwtService, props);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest(null,"pw"));
        assertEquals(400, resp.getStatusCode().value());
    }
}
