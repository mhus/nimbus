package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentifizierungsanfrage von Client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    private String username;
    private String password;
    private String clientInfo;
}
