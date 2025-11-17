package de.mhus.nimbus.generated.network;

import de.mhus.nimbus.generated.types.WorldInfo;
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
public class LoginResponseData {

    /**
     * success
     */
    private boolean success;

    /**
     * userId
     */
    private String userId;

    /**
     * displayName
     */
    private String displayName;

    /**
     * worldInfo
     */
    private WorldInfo worldInfo;

    /**
     * sessionId
     */
    private String sessionId;
}
