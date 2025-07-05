package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.config.JwtProperties;
import de.mhus.nimbus.identity.service.JwtService;
import de.mhus.nimbus.identity.service.LoginService;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.LoginStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST Controller für Login-Operationen
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(LoginService loginService, JwtService jwtService, JwtProperties jwtProperties) {
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Login-Endpunkt für REST-API
     */
    @PostMapping("/login")
    public ResponseEntity<LoginRestResponse> login(@RequestBody LoginRestRequest request) {
        try {
            // Konvertiere REST-Request zu Avro-Request
            LoginRequest loginRequest = LoginRequest.newBuilder()
                    .setRequestId(UUID.randomUUID().toString())
                    .setUsername(request.username())
                    .setPassword(request.password())
                    .setTimestamp(Instant.now())
                    .setClientInfo("REST-API")
                    .build();

            // Validiere und verarbeite die Anfrage
            loginService.validateLoginRequest(loginRequest);
            LoginResponse response = loginService.processLoginRequest(loginRequest);

            // Konvertiere Avro-Response zu REST-Response
            LoginRestResponse restResponse = convertToRestResponse(response);

            // Bestimme HTTP-Status basierend auf Login-Status
            HttpStatus httpStatus = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case INVALID_CREDENTIALS, USER_NOT_FOUND -> HttpStatus.UNAUTHORIZED;
                case USER_INACTIVE -> HttpStatus.FORBIDDEN;
                case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            };

            return ResponseEntity.status(httpStatus).body(restResponse);

        } catch (IllegalArgumentException e) {
            LoginRestResponse errorResponse = new LoginRestResponse(
                    false, null, null, null, e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            LoginRestResponse errorResponse = new LoginRestResponse(
                    false, null, null, null, "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Token-Validierung Endpunkt
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request) {
        // Hier könnte eine Token-Validierung implementiert werden
        // Für jetzt return wir eine einfache Response
        return ResponseEntity.ok(new TokenValidationResponse(true, "Token is valid"));
    }

    /**
     * Öffentlicher Schlüssel Endpunkt für andere Services
     */
    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey() {
        try {
            String publicKeyString = jwtService.getPublicKeyAsString();
            String issuer = jwtProperties.getIssuer();

            PublicKeyResponse response = new PublicKeyResponse(
                    publicKeyString,
                    "RSA",
                    "RS256",
                    issuer,
                    "Use this public key to validate JWT tokens issued by this service"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PublicKeyResponse(null, null, null, null, "Error retrieving public key"));
        }
    }

    /**
     * Konvertiert LoginResponse zu REST-Response
     */
    private LoginRestResponse convertToRestResponse(LoginResponse response) {
        boolean success = response.getStatus() == LoginStatus.SUCCESS;
        String token = response.getToken();
        Instant expiresAt = response.getExpiresAt();

        UserInfoRest userInfo = null;
        if (response.getUser() != null) {
            userInfo = new UserInfoRest(
                    response.getUser().getId(),
                    response.getUser().getUsername(),
                    response.getUser().getEmail(),
                    response.getUser().getFirstName(),
                    response.getUser().getLastName()
            );
        }

        String message = success ? "Login successful" : response.getErrorMessage();

        return new LoginRestResponse(success, token, expiresAt.toEpochMilli(), userInfo, message);
    }

    // Request/Response DTOs für REST API
    public record LoginRestRequest(
            String username,
            String password
    ) {}

    public record LoginRestResponse(
            boolean success,
            String token,
            Long expiresAt,
            UserInfoRest user,
            String message
    ) {}

    public record UserInfoRest(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName
    ) {}

    public record TokenValidationRequest(
            String token
    ) {}

    public record TokenValidationResponse(
            boolean valid,
            String message
    ) {}

    public record PublicKeyResponse(
            String publicKey,
            String keyType,
            String algorithm,
            String issuer,
            String description
    ) {}
}
