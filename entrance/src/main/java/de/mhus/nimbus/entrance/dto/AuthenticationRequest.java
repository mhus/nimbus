package de.mhus.nimbus.entrance.dto;

/**
 * Diese Datei wurde nach de.mhus.nimbus.shared.dto verschoben
 */

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
