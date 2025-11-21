/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'interface PlayerMovementStateChangedEvent'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class PlayerMovementStateChangedEvent extends Object {
    private String playerId;
    private PlayerMovementState oldState;
    private PlayerMovementState newState;
}
