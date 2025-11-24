/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'interface PlayerMovementStateChangedEvent'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerMovementStateChangedEvent {
    @Deprecated
    @SuppressWarnings("required")
    private String playerId;
    @Deprecated
    @SuppressWarnings("required")
    private PlayerMovementState oldState;
    @Deprecated
    @SuppressWarnings("required")
    private PlayerMovementState newState;
}
