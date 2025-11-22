package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.universe.security.USecurityProperties;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ULoginControllerTest {

    @Test
    void login_success() throws Exception {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1"); user.setRolesRaw("admin");
        Mockito.when(userService.getByUsername("user1")).thenReturn(Optional.of(user));
        Mockito.when(userService.validatePassword("u1", "pw")).thenReturn(true);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC"); kpg.initialize(256); KeyPair pair = kpg.generateKeyPair();
        Mockito.when(keyService.getLatestPrivateKey(KeyType.UNIVERSE, "system")).thenReturn(Optional.of(pair.getPrivate()));
        Mockito.when(jwtService.createTokenWithSecretKey(
                Mockito.eq(pair.getPrivate()),
                Mockito.eq("u1"),
                Mockito.eq(Map.of("username","user1","universe","admin", "typ","access")),
                Mockito.any()
        )).thenReturn("accessTok");
        Mockito.when(jwtService.createTokenWithSecretKey(
                Mockito.eq(pair.getPrivate()),
                Mockito.eq("u1"),
                Mockito.eq(Map.of("username","u1","typ","refresh")),
                Mockito.any()
        )).thenReturn("refreshTok");
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user1","pw"));
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("accessTok", resp.getBody().token());
        assertEquals("refreshTok", resp.getBody().refreshToken());
    }

    @Test
    void refresh_success() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1"); user.setRolesRaw("admin");
        Mockito.when(userService.getById("u1")).thenReturn(Optional.of(user));
        @SuppressWarnings("unchecked") Jws<Claims> jws = Mockito.mock(Jws.class);
        Claims claims = Mockito.mock(Claims.class);
        Mockito.when(jws.getPayload()).thenReturn(claims);
        Mockito.when(claims.getSubject()).thenReturn("u1");
        Mockito.when(claims.get("username", String.class)).thenReturn("user1");
        Mockito.when(jwtService.validateTokenWithPublicKey("oldRefresh", KeyType.UNIVERSE, "system")).thenReturn(Optional.of(jws));
        Mockito.when(claims.get("typ")).thenReturn("refresh");
        PrivateKey priv = Mockito.mock(PrivateKey.class);
        Mockito.when(priv.getAlgorithm()).thenReturn("EC");
        Mockito.when(keyService.getLatestPrivateKey(KeyType.UNIVERSE, "system")).thenReturn(Optional.of(priv));
        Mockito.when(jwtService.createTokenWithSecretKey(
                Mockito.eq(priv),
                Mockito.eq("u1"),
                Mockito.eq(Map.of("username","user1","universe","admin", "typ","access")),
                Mockito.any()
        )).thenReturn("newAccess");
        Mockito.when(jwtService.createTokenWithSecretKey(
                Mockito.eq(priv),
                Mockito.eq("u1"),
                Mockito.eq(Map.of("username","u1","typ","refresh")),
                Mockito.any()
        )).thenReturn("newRefresh");
        ResponseEntity<ULoginResponse> resp = controller.refresh("Bearer oldRefresh");
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("newAccess", resp.getBody().token());
        assertEquals("newRefresh", resp.getBody().refreshToken());
    }

    @Test
    void login_badRequest_nullPassword() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user", null));
        assertEquals(400, resp.getStatusCodeValue());
    }

    @Test
    void login_unauthorized_wrongPassword() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1");
        Mockito.when(userService.getByUsername("user1")).thenReturn(Optional.of(user));
        Mockito.when(userService.validatePassword("u1", "wrong")).thenReturn(false);
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user1", "wrong"));
        assertEquals(401, resp.getStatusCodeValue());
    }

    @Test
    void refresh_missingHeader() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        ResponseEntity<ULoginResponse> resp = controller.refresh(null);
        assertEquals(401, resp.getStatusCodeValue());
    }

    @Test
    void refresh_invalidToken() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        Mockito.when(jwtService.validateTokenWithPublicKey("old", KeyType.UNIVERSE, "system")).thenReturn(Optional.empty());
        ResponseEntity<ULoginResponse> resp = controller.refresh("Bearer old");
        assertEquals(401, resp.getStatusCodeValue());
    }

    @Test
    void login_unauthorized_disabledUser() throws Exception {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        USecurityProperties props = new USecurityProperties();
        KeyService keyService = Mockito.mock(KeyService.class);
        ULoginController controller = new ULoginController(userService, jwtService, props, keyService);
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1"); user.setEnabled(false);
        Mockito.when(userService.getByUsername("user1")).thenReturn(Optional.of(user));
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user1","pw"));
        assertEquals(401, resp.getStatusCodeValue());
    }
}
