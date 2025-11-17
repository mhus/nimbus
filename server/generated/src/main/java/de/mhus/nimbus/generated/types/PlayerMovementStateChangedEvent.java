package de.mhus.nimbus.generated.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from PlayerMovementState.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMovementStateChangedEvent {

    /**
     * playerId
     */
    private String playerId;

    /**
     * oldState
     */
    private PlayerMovementState oldState;

    /**
     * newState
     */
    private PlayerMovementState newState;
}
