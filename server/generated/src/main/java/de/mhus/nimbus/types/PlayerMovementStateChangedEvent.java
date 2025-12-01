/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'interface PlayerMovementStateChangedEvent'
 */
package de.mhus.nimbus.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerMovementStateChangedEvent {
    private String playerId;
    private PlayerMovementState oldState;
    private PlayerMovementState newState;
}
