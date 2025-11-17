package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.JwtProperties;
import de.mhus.nimbus.universe.user.UserService;
import de.mhus.nimbus.universe.user.User;
import de.mhus.nimbus.shared.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UserService userService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public LoginController(UserService userService, JwtService jwtService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

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
}

