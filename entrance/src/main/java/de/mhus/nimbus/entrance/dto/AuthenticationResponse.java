package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentifizierungsantwort an Client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private boolean success;
    private String token;
    private String message;
    private String username;
}
