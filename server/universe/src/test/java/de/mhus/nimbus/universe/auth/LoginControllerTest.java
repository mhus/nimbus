package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.JwtProperties;
import de.mhus.nimbus.universe.user.User;
import de.mhus.nimbus.universe.user.UserService;
import de.mhus.nimbus.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
        UserService userService = mock(UserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");
        props.setExpiresMinutes(60);

        User user = new User("alpha","alpha@example.com");
        user.setId("u1");
        when(userService.getByUsername("alpha")).thenReturn(Optional.of(user));
        when(userService.validatePassword("u1","pw"))
                .thenReturn(true);
        when(jwtService.createTokenWithSecretKey(eq(props.getKeyId()), eq("u1"), any(Map.class), any(Instant.class)))
                .thenReturn("jwt-token");

        LoginController controller = new LoginController(userService, jwtService, props);
        ResponseEntity<LoginResponse> resp = controller.login(new LoginRequest("alpha","pw"));
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("jwt-token", resp.getBody().token());
        assertEquals("u1", resp.getBody().userId());
    }

    @Test
    void login_wrong_password() {
        UserService userService = mock(UserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");

        User user = new User("alpha","alpha@example.com");
        user.setId("u1");
        when(userService.getByUsername("alpha")).thenReturn(Optional.of(user));
        when(userService.validatePassword("u1","bad"))
                .thenReturn(false);

        LoginController controller = new LoginController(userService, jwtService, props);
        ResponseEntity<LoginResponse> resp = controller.login(new LoginRequest("alpha","bad"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void login_user_not_found() {
        UserService userService = mock(UserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");

        when(userService.getByUsername("alpha")).thenReturn(Optional.empty());
        LoginController controller = new LoginController(userService, jwtService, props);
        ResponseEntity<LoginResponse> resp = controller.login(new LoginRequest("alpha","pw"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void login_bad_request() {
        UserService userService = mock(UserService.class);
        JwtService jwtService = mock(JwtService.class);
        JwtProperties props = new JwtProperties();
        props.setKeyId("system:authkey");
        props.setSecretBase64("c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0");
        LoginController controller = new LoginController(userService, jwtService, props);
        ResponseEntity<LoginResponse> resp = controller.login(new LoginRequest(null,"pw"));
        assertEquals(400, resp.getStatusCode().value());
    }
}
