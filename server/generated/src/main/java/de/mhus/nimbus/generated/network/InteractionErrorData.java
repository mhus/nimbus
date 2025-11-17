package de.mhus.nimbus.generated.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from InteractionMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionErrorData {

    /**
     * success
     */
    private boolean success;

    /**
     * errorCode
     */
    private double errorCode;

    /**
     * errorMessage
     */
    private String errorMessage;
}
