package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.universe.security.USecurityProperties;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping(ULoginController.BASE_PATH)
@Tag(name = "Auth", description = "Authentication operations")
@RequiredArgsConstructor
public class ULoginController {

    public static final String BASE_PATH = "/universe/user/auth";
    private static final String OWNER_SYSTEM = "system"; // Owner für Universe Keys

    private final UUserService userService;
    private final JwtService jwtService;
    private final USecurityProperties jwtProperties;
    private final KeyService keyService;

    @Operation(summary = "Login with username/password", description = "Returns JWT bearer token on success")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/login")
    public ResponseEntity<ULoginResponse> login(@RequestBody ULoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }
        var userOpt = userService.getByUsername(request.username());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        UUser user = userOpt.get();
        if (!user.isEnabled()) {
            return ResponseEntity.status(401).build();
        }
        boolean valid = userService.validatePassword(user.getId(), request.password());
        if (!valid) {
            return ResponseEntity.status(401).build();
        }
        Instant exp = Instant.now().plus(jwtProperties.getAuthExpiresMinutes(), ChronoUnit.MINUTES);
        Instant refreshExp = Instant.now().plus(jwtProperties.getRefreshExpiresDays(), ChronoUnit.DAYS);
        String rolesRaw = user.getRolesRaw();
        Map<String,Object> claims = rolesRaw == null ? Map.of("username", user.getUsername(), "typ","access") : Map.of("username", user.getUsername(), "universe", rolesRaw, "typ","access");
        // Private Key laden
        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.UNIVERSE, OWNER_SYSTEM);
        if (privateKeyOpt.isEmpty()) {
            return ResponseEntity.status(500).build();
        }
        String accessToken = createAccessToken(privateKeyOpt.get(), user.getId(), claims, exp);
        String refreshToken = createRefreshToken(privateKeyOpt.get(), user.getId(), refreshExp);
        return ResponseEntity.ok(new ULoginResponse(accessToken, refreshToken, user.getId(), user.getUsername()));
    }

    @Operation(summary = "Logout (stateless)", description = "No server action, provided for client flow")
    @ApiResponse(responseCode = "200", description = "Logout acknowledged")
    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Refresh JWT token", description = "Requires valid bearer token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ULoginResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String refreshToken = authorization.substring(7).trim();
        // Validierung über Public Keys
        var claimsOpt = jwtService.validateTokenWithPublicKey(refreshToken, KeyType.UNIVERSE, OWNER_SYSTEM);
        if (claimsOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        var payload = claimsOpt.get().getPayload();
        if ("refresh".equals(payload.get("typ")) == false) {
            return ResponseEntity.status(401).build();
        }
        String userId = payload.getSubject();
        String username = payload.get("username", String.class);
        UUser user = userService.getById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            return ResponseEntity.status(401).build();
        }
        String rolesRaw = user != null ? user.getRolesRaw() : null;
        Instant exp = Instant.now().plus(jwtProperties.getAuthExpiresMinutes(), ChronoUnit.MINUTES);
        Instant newRefreshExp = Instant.now().plus(jwtProperties.getRefreshExpiresDays(), ChronoUnit.DAYS);
        Map<String,Object> newClaims = rolesRaw == null ? Map.of("username", username, "typ","access") : Map.of("username", username, "universe", rolesRaw, "typ","access");
        var privateKeyOpt = keyService.getLatestPrivateKey(KeyType.UNIVERSE, OWNER_SYSTEM);
        if (privateKeyOpt.isEmpty()) {
            return ResponseEntity.status(500).build();
        }
        String newAccessToken = createAccessToken(privateKeyOpt.get(), userId, newClaims, exp);
        String newRefreshToken = createRefreshToken(privateKeyOpt.get(), userId, newRefreshExp);
        return ResponseEntity.ok(new ULoginResponse(newAccessToken, newRefreshToken, userId, username));
    }

    private String createAccessToken(PrivateKey key, String subject, Map<String,Object> claims, Instant exp) {
        return jwtService.createTokenWithSecretKey(key, subject, claims, exp);
    }

    private String createRefreshToken(PrivateKey key, String subject, Instant exp) {
        return jwtService.createTokenWithSecretKey(key, subject, Map.of("username", subject, "typ","refresh"), exp);
    }
}
