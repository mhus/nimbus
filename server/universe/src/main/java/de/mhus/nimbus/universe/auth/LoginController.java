package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.JwtProperties;
import de.mhus.nimbus.universe.user.UserService;
import de.mhus.nimbus.universe.user.User;
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
@RequestMapping(LoginController.BASE_PATH)
@Tag(name = "Auth", description = "Authentication operations")
public class LoginController {

    public static final String BASE_PATH = "/universe/user/auth";

    private final UserService userService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public LoginController(UserService userService, JwtService jwtService, JwtProperties jwtProperties) {
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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }
        var userOpt = userService.getByUsername(request.username());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        User user = userOpt.get();
        boolean valid = userService.validatePassword(user.getId(), request.password());
        if (!valid) {
            return ResponseEntity.status(401).build();
        }
        Instant exp = Instant.now().plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);
        String token = jwtService.createTokenWithSecretKey(
                jwtProperties.getKeyId(),
                user.getId(),
                Map.of("username", user.getUsername()),
                exp
        );
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getUsername()));
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
    public ResponseEntity<LoginResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
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
        // Issue new token with same subject & username, new expiration
        Instant exp = Instant.now().plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);
        String newToken = jwtService.createTokenWithSecretKey(
                jwtProperties.getKeyId(),
                userId,
                Map.of("username", username),
                exp
        );
        return ResponseEntity.ok(new LoginResponse(newToken, userId, username));
    }
}
