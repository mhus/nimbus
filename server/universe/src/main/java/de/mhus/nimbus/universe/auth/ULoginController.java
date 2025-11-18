package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.JwtProperties;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.security.JwtService;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping(ULoginController.BASE_PATH)
@Tag(name = "Auth", description = "Authentication operations")
public class ULoginController {

    public static final String BASE_PATH = "/universe/user/auth";

    private final UUserService userService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public ULoginController(UUserService userService, JwtService jwtService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

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
        boolean valid = userService.validatePassword(user.getId(), request.password());
        if (!valid) {
            return ResponseEntity.status(401).build();
        }
        Instant exp = Instant.now().plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);
        String rolesRaw = user.getRolesRaw();
        Map<String,Object> claims = rolesRaw == null ? Map.of("username", user.getUsername()) : Map.of("username", user.getUsername(), "universe", rolesRaw);
        String token = jwtService.createTokenWithSecretKey(
                jwtProperties.getKeyId(),
                user.getId(),
                claims,
                exp
        );
        return ResponseEntity.ok(new ULoginResponse(token, user.getId(), user.getUsername()));
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
        String oldToken = authorization.substring(7).trim();
        var claimsOpt = jwtService.validateTokenWithSecretKey(oldToken, jwtProperties.getKeyId());
        if (claimsOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        var claims = claimsOpt.get().getPayload();
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        UUser user = userService.getById(userId).orElse(null);
        String rolesRaw = user != null ? user.getRolesRaw() : null;
        Instant exp = Instant.now().plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);
        Map<String,Object> newClaims = rolesRaw == null ? Map.of("username", username) : Map.of("username", username, "universe", rolesRaw);
        String newToken = jwtService.createTokenWithSecretKey(
                jwtProperties.getKeyId(),
                userId,
                newClaims,
                exp
        );
        return ResponseEntity.ok(new ULoginResponse(newToken, userId, username));
    }
}
