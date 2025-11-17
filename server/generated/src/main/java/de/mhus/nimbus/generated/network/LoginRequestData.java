package de.mhus.nimbus.generated.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from LoginMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestData {

    /**
     * username (optional)
     */
    private String username;

    /**
     * password (optional)
     */
    private String password;

    /**
     * token (optional)
     */
    private String token;

    /**
     * worldId
     */
    private String worldId;

    /**
     * clientType
     */
    private ClientType clientType;

    /**
     * sessionId (optional)
     */
    private String sessionId;
}
