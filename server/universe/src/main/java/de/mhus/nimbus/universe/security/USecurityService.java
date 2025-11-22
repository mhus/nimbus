package de.mhus.nimbus.universe.security;

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

/**
 * Kapselt die Business-Logik für Login und Refresh. Controller ruft nur noch diesen Service.
 */
@Service
@RequiredArgsConstructor
public class USecurityService {

    private final UUserService userService;
    private final JwtService jwtService;
    private final USecurityProperties securityProperties;
    private final KeyService keyService;

    /** Ergebnis-Kapsel mit HTTP-Status und optionalem Payload. */
    public record AuthResult(HttpStatus status, ULoginResponse payload) {
        public boolean isOk() { return status == HttpStatus.OK; }
        public static AuthResult ok(ULoginResponse p) { return new AuthResult(HttpStatus.OK, p); }
        public static AuthResult of(HttpStatus s) { return new AuthResult(s, null); }
    }

    public AuthResult login(ULoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return AuthResult.of(HttpStatus.BAD_REQUEST);
        }
        Optional<UUser> userOpt = userService.getByUsername(request.username());
        if (userOpt.isEmpty()) return AuthResult.of(HttpStatus.UNAUTHORIZED);
        UUser user = userOpt.get();
        if (!user.isEnabled()) return AuthResult.of(HttpStatus.UNAUTHORIZED);
        boolean valid = userService.validatePassword(user.getId(), request.password());
        if (!valid) return AuthResult.of(HttpStatus.UNAUTHORIZED);

        Instant accessExp = Instant.now().plus(securityProperties.getAuthExpiresMinutes(), ChronoUnit.MINUTES);
        Instant refreshExp = Instant.now().plus(securityProperties.getRefreshExpiresDays(), ChronoUnit.DAYS);
        Instant loginAt = Instant.now();
        long loginAtEpoch = loginAt.toEpochMilli();
        String rolesRaw = user.getRolesRaw();
        Map<String,Object> claims = rolesRaw == null ?
                Map.of("username", user.getUsername(), "typ","access", "loginAt", loginAtEpoch) :
                Map.of("username", user.getUsername(), "universe", rolesRaw, "typ","access", "loginAt", loginAtEpoch);

        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.UNIVERSE, securityProperties.JWT_TOKEN_KEY_ID);
        if (privateKeyOpt.isEmpty()) return AuthResult.of(HttpStatus.INTERNAL_SERVER_ERROR);
        PrivateKey privateKey = privateKeyOpt.get();
        String accessToken = jwtService.createTokenWithSecretKey(privateKey, user.getId(), claims, accessExp);
        String refreshToken = jwtService.createTokenWithSecretKey(privateKey, user.getId(), Map.of("username", user.getId(), "typ","refresh", "loginAt", loginAtEpoch), refreshExp);
        return AuthResult.ok(new ULoginResponse(accessToken, refreshToken, user.getId(), user.getUsername()));
    }

    public AuthResult refresh(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return AuthResult.of(HttpStatus.UNAUTHORIZED);
        }
        String refreshToken = authorizationHeader.substring(7).trim();
        var claimsOpt = jwtService.validateTokenWithPublicKey(refreshToken, KeyType.UNIVERSE, securityProperties.JWT_TOKEN_KEY_ID);
        if (claimsOpt.isEmpty()) return AuthResult.of(HttpStatus.UNAUTHORIZED);
        var payload = claimsOpt.get().getPayload();
        if (!"refresh".equals(payload.get("typ"))) return AuthResult.of(HttpStatus.UNAUTHORIZED);

        String userId = payload.getSubject();
        String username = payload.get("username", String.class);
        UUser user = userService.getById(userId).orElse(null);
        if (user == null || !user.isEnabled()) return AuthResult.of(HttpStatus.UNAUTHORIZED);
        String rolesRaw = user.getRolesRaw();

        Object loginAtObj = payload.get("loginAt");
        if (!(loginAtObj instanceof Number)) {
            return AuthResult.of(HttpStatus.UNAUTHORIZED); // fehlender Ursprung
        }
        long loginAtEpoch = ((Number)loginAtObj).longValue();
        Instant loginAt = Instant.ofEpochMilli(loginAtEpoch);
        long maxDays = securityProperties.getRefreshMaxTotalDays();
        Instant cutoff = loginAt.plus(maxDays, ChronoUnit.DAYS);
        if (Instant.now().isAfter(cutoff)) {
            return AuthResult.of(HttpStatus.UNAUTHORIZED); // Gesamtzeit überschritten
        }

        Instant accessExp = Instant.now().plus(securityProperties.getAuthExpiresMinutes(), ChronoUnit.MINUTES);
        Instant newRefreshExp = Instant.now().plus(securityProperties.getRefreshExpiresDays(), ChronoUnit.DAYS);
        Map<String,Object> claims = rolesRaw == null ?
                Map.of("username", username, "typ","access", "loginAt", loginAtEpoch) :
                Map.of("username", username, "universe", rolesRaw, "typ","access", "loginAt", loginAtEpoch);

        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.UNIVERSE, securityProperties.JWT_TOKEN_KEY_ID);
        if (privateKeyOpt.isEmpty()) return AuthResult.of(HttpStatus.INTERNAL_SERVER_ERROR);
        PrivateKey privateKey = privateKeyOpt.get();
        String newAccess = jwtService.createTokenWithSecretKey(privateKey, userId, claims, accessExp);
        String newRefresh = jwtService.createTokenWithSecretKey(privateKey, userId, Map.of("username", userId, "typ","refresh", "loginAt", loginAtEpoch), newRefreshExp);
        return AuthResult.ok(new ULoginResponse(newAccess, newRefresh, userId, username));
    }
}
