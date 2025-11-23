package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.UniverseProperties;
import de.mhus.nimbus.universe.auth.ULoginRequest;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class USecurityServiceTest {

    @Test
    void loginAddsLoginAtClaim() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwt = Mockito.mock(JwtService.class);
        KeyService keyService = Mockito.mock(KeyService.class);
        UniverseProperties props = new UniverseProperties();
        PrivateKey privateKey = Mockito.mock(PrivateKey.class);
        Mockito.when(privateKey.getAlgorithm()).thenReturn("EC");
        Mockito.when(keyService.getLatestPrivateKey(KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT)).thenReturn(Optional.of(privateKey));
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1"); user.setEnabled(true);
        Mockito.when(userService.getByUsername("user1")).thenReturn(Optional.of(user));
        Mockito.when(userService.validatePassword("u1","pw")).thenReturn(true);
        USecurityService service = new USecurityService(userService, jwt, props, keyService);
        Mockito.when(jwt.createTokenWithSecretKey(Mockito.eq(privateKey), Mockito.eq("u1"), Mockito.anyMap(), Mockito.any()))
                .thenReturn("tok1", "tok2");
        var result = service.login(new ULoginRequest("user1","pw"));
        assertTrue(result.isOk());
        ArgumentCaptor<Map<String,Object>> claimsCap = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(jwt, Mockito.times(2)).createTokenWithSecretKey(Mockito.eq(privateKey), Mockito.eq("u1"), claimsCap.capture(), Mockito.any());
        Map<String,Object> accessClaims = claimsCap.getAllValues().get(0);
        assertEquals("access", accessClaims.get("typ"));
        assertTrue(accessClaims.containsKey("loginAt"));
        assertTrue(accessClaims.get("loginAt") instanceof Long);
    }

    @Test
    void refreshKeepsLoginAtAndBlocksAfterMaxDays() {
        UUserService userService = Mockito.mock(UUserService.class);
        JwtService jwt = Mockito.mock(JwtService.class);
        KeyService keyService = Mockito.mock(KeyService.class);
        UniverseProperties props = new UniverseProperties();
        props.setSecurityRefreshMaxTotalDays(1); // sehr kurz
        PrivateKey privateKey = Mockito.mock(PrivateKey.class);
        Mockito.when(privateKey.getAlgorithm()).thenReturn("EC");
        Mockito.when(keyService.getLatestPrivateKey(KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT)).thenReturn(Optional.of(privateKey));
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1"); user.setEnabled(true);
        Mockito.when(userService.getById("u1")).thenReturn(Optional.of(user));

        // JWS/Claims für gültigen Refresh innerhalb Zeit
        @SuppressWarnings("unchecked") Jws<Claims> jwsValid = Mockito.mock(Jws.class);
        Claims claimsValid = Mockito.mock(Claims.class);
        Mockito.when(jwsValid.getPayload()).thenReturn(claimsValid);
        long loginAt = Instant.now().minus(12, ChronoUnit.HOURS).toEpochMilli();
        Mockito.when(claimsValid.get("typ")).thenReturn("refresh");
        Mockito.when(claimsValid.get("username", String.class)).thenReturn("user1");
        Mockito.when(claimsValid.getSubject()).thenReturn("u1");
        Mockito.when(claimsValid.get("loginAt")).thenReturn(loginAt);
        Mockito.when(jwt.validateTokenWithPublicKey("good", KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT)).thenReturn(Optional.of(jwsValid));

        // JWS/Claims für abgelaufenen Refresh (nach max Tage)
        @SuppressWarnings("unchecked") Jws<Claims> jwsExpired = Mockito.mock(Jws.class);
        Claims claimsExpired = Mockito.mock(Claims.class);
        Mockito.when(jwsExpired.getPayload()).thenReturn(claimsExpired);
        long oldLoginAt = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli();
        Mockito.when(claimsExpired.get("typ")).thenReturn("refresh");
        Mockito.when(claimsExpired.get("username", String.class)).thenReturn("user1");
        Mockito.when(claimsExpired.getSubject()).thenReturn("u1");
        Mockito.when(claimsExpired.get("loginAt")).thenReturn(oldLoginAt);
        Mockito.when(jwt.validateTokenWithPublicKey("expired", KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT)).thenReturn(Optional.of(jwsExpired));

        Mockito.when(jwt.createTokenWithSecretKey(Mockito.eq(privateKey), Mockito.eq("u1"), Mockito.anyMap(), Mockito.any()))
                .thenReturn("newAccess", "newRefresh");

        USecurityService service = new USecurityService(userService, jwt, props, keyService);
        var okResult = service.refresh("Bearer good");
        assertTrue(okResult.isOk());
        var expiredResult = service.refresh("Bearer expired");
        assertFalse(expiredResult.isOk());
        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, expiredResult.status());
    }
}
