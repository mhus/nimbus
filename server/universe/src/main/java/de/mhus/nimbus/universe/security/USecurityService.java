package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.UniverseProperties;
import de.mhus.nimbus.universe.auth.ULoginRequest;
import de.mhus.nimbus.universe.auth.ULoginResponse;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Kapselt die Business-Logik für Login und Refresh. Controller ruft nur noch diesen Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class USecurityService {

    private final UUserService userService;
    private final JwtService jwtService;
    private final UniverseProperties securityProperties;
    private final KeyService keyService;

    /** Ergebnis-Kapsel mit HTTP-Status und optionalem Payload. */
    public record AuthResult(HttpStatus status, ULoginResponse payload) {
        public boolean isOk() { return status == HttpStatus.OK; }
        public static AuthResult ok(ULoginResponse p) { return new AuthResult(HttpStatus.OK, p); }
        public static AuthResult of(HttpStatus s) { return new AuthResult(s, null); }
    }

    public AuthResult login(ULoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            log.warn("Login fehlgeschlagen: fehlende Felder request={}", request);
            return AuthResult.of(HttpStatus.BAD_REQUEST);
        }
        Optional<UUser> userOpt = userService.getByUsername(request.username());
        if (userOpt.isEmpty()) {
            log.warn("Login fehlgeschlagen: Benutzer '{}' nicht gefunden", request.username());
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        UUser user = userOpt.get();
        if (!user.isEnabled()) {
            log.warn("Login fehlgeschlagen: Benutzer '{}' ist deaktiviert", user.getUsername());
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        boolean valid = userService.validatePassword(user.getId(), request.password());
        if (!valid) {
            log.warn("Login fehlgeschlagen: Passwort ungültig für Benutzer '{}'", user.getUsername());
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }

        Instant accessExp = Instant.now().plus(securityProperties.getSecurityAuthExpiresMinutes(), ChronoUnit.MINUTES);
        Instant refreshExp = Instant.now().plus(securityProperties.getSecurityRefreshExpiresDays(), ChronoUnit.DAYS);
        Instant loginAt = Instant.now();
        long loginAtEpoch = loginAt.toEpochMilli();
        String rolesRaw = user.getRolesAsString();
        Map<String,Object> claims = rolesRaw == null ?
                Map.of("username", user.getUsername(), "typ","access", "loginAt", loginAtEpoch) :
                Map.of("username", user.getUsername(), "universe", rolesRaw, "typ","access", "loginAt", loginAtEpoch);

        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT);
        if (privateKeyOpt.isEmpty()) {
            log.error("Login intern fehlgeschlagen: Kein PrivateKey für UNIVERSE owner='{}' gefunden", UniverseProperties.MAIN_JWT_TOKEN_INTENT);
            return AuthResult.of(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        PrivateKey privateKey = privateKeyOpt.get();
        try {
            String accessToken = jwtService.createTokenWithPrivateKey(privateKey, user.getId(), claims, accessExp);
            String refreshToken = jwtService.createTokenWithPrivateKey(privateKey, user.getId(), Map.of("username", user.getId(), "typ","refresh", "loginAt", loginAtEpoch), refreshExp);
            log.info("Login erfolgreich für Benutzer '{}'", user.getUsername());
            return AuthResult.ok(new ULoginResponse(accessToken, refreshToken, user.getId(), user.getUsername()));
        } catch (Exception e) {
            log.error("Login Token-Erstellung schlug fehl für Benutzer '{}'", user.getUsername(), e);
            return AuthResult.of(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public AuthResult refresh(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Refresh fehlgeschlagen: fehlender oder ungültiger Authorization Header");
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        String refreshToken = authorizationHeader.substring(7).trim();
        var claimsOpt = jwtService.validateTokenWithPublicKey(refreshToken, KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT);
        if (claimsOpt.isEmpty()) {
            log.warn("Refresh fehlgeschlagen: Token ungültig (Signatur/Struktur)");
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        var payload = claimsOpt.get().getPayload();
        if (!"refresh".equals(payload.get("typ"))) {
            log.warn("Refresh fehlgeschlagen: falscher typ='{}'", payload.get("typ"));
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }

        String userId = payload.getSubject();
        String username = payload.get("username", String.class);
        UUser user = userService.getById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            log.warn("Refresh fehlgeschlagen: Benutzer '{}' nicht gefunden oder deaktiviert", userId);
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        String rolesRaw = user.getRolesAsString();

        Object loginAtObj = payload.get("loginAt");
        if (!(loginAtObj instanceof Number)) {
            log.warn("Refresh fehlgeschlagen: loginAt Claim fehlt oder ungültig");
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        long loginAtEpoch = ((Number)loginAtObj).longValue();
        Instant loginAt = Instant.ofEpochMilli(loginAtEpoch);
        long maxDays = securityProperties.getSecurityRefreshMaxTotalDays();
        Instant cutoff = loginAt.plus(maxDays, ChronoUnit.DAYS);
        if (Instant.now().isAfter(cutoff)) {
            log.info("Refresh blockiert: Gesamtlebensdauer überschritten für Benutzer '{}' (loginAt={}, maxDays={})", user.getUsername(), loginAt, maxDays);
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }

        Instant accessExp = Instant.now().plus(securityProperties.getSecurityAuthExpiresMinutes(), ChronoUnit.MINUTES);
        Instant newRefreshExp = Instant.now().plus(securityProperties.getSecurityRefreshExpiresDays(), ChronoUnit.DAYS);
        Map<String,Object> claims = rolesRaw == null ?
                Map.of("username", username, "typ","access", "loginAt", loginAtEpoch) :
                Map.of("username", username, "universe", rolesRaw, "typ","access", "loginAt", loginAtEpoch);

        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT);
        if (privateKeyOpt.isEmpty()) {
            log.error("Refresh intern fehlgeschlagen: Kein PrivateKey für UNIVERSE owner='{}' gefunden", UniverseProperties.MAIN_JWT_TOKEN_INTENT);
            return AuthResult.of(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        PrivateKey privateKey = privateKeyOpt.get();
        try {
            String newAccess = jwtService.createTokenWithPrivateKey(privateKey, userId, claims, accessExp);
            String newRefresh = jwtService.createTokenWithPrivateKey(privateKey, userId, Map.of("username", userId, "typ","refresh", "loginAt", loginAtEpoch), newRefreshExp);
            log.info("Refresh erfolgreich für Benutzer '{}'", username);
            return AuthResult.ok(new ULoginResponse(newAccess, newRefresh, userId, username));
        } catch (Exception e) {
            log.error("Refresh Token-Erstellung schlug fehl für Benutzer '{}'", username, e);
            return AuthResult.of(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
