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
        Mockito.when(jwtService.createTokenWithSecretKey(pair.getPrivate(), "u1", Map.of("username","user1","universe","admin"), Mockito.any())).thenReturn("tok");
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user1","pw"));
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("tok", resp.getBody().token());
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
        Mockito.when(jwtService.validateTokenWithPublicKey("old", KeyType.UNIVERSE, "system")).thenReturn(Optional.of(jws));
        PrivateKey priv = Mockito.mock(PrivateKey.class);
        Mockito.when(priv.getAlgorithm()).thenReturn("EC");
        Mockito.when(keyService.getLatestPrivateKey(KeyType.UNIVERSE, "system")).thenReturn(Optional.of(priv));
        Mockito.when(jwtService.createTokenWithSecretKey(priv, "u1", Map.of("username","user1","universe","admin"), Mockito.any())).thenReturn("newTok");
        ResponseEntity<ULoginResponse> resp = controller.refresh("Bearer old");
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("newTok", resp.getBody().token());
    }
}

