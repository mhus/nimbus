/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'interface PlayerMovementStateChangedEvent'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerMovementStateChangedEvent {
    private String playerId;
    private PlayerMovementState oldState;
    private PlayerMovementState newState;
}
