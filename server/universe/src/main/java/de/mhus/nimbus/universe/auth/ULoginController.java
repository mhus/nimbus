package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.USecurityProperties;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.universe.security.USecurityService;
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

@RestController
@RequestMapping(ULoginController.BASE_PATH)
@Tag(name = "Auth", description = "Authentication operations")
@RequiredArgsConstructor
public class ULoginController {

    public static final String BASE_PATH = "/universe/user/auth";
    private final USecurityService securityService;

    @Operation(summary = "Login with username/password", description = "Returns JWT bearer token on success")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/login")
    public ResponseEntity<ULoginResponse> login(@RequestBody ULoginRequest request) {
        var result = securityService.login(request);
        return result.isOk() ? ResponseEntity.ok(result.payload()) : ResponseEntity.status(result.status()).build();
    }

    @Operation(summary = "Refresh JWT token", description = "Requires valid bearer token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ULoginResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var result = securityService.refresh(authorization);
        return result.isOk() ? ResponseEntity.ok(result.payload()) : ResponseEntity.status(result.status()).build();
    }
}
