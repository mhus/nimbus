/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'enum PlayerMovementState'
 */
package de.mhus.nimbus.generated.types;

public enum PlayerMovementState {
    WALK("WALK"),
    SPRINT("SPRINT"),
    JUMP("JUMP"),
    FALL("FALL"),
    FREE_FLY("FREE_FLY"),
    FLY("FLY"),
    SWIM("SWIM"),
    CROUCH("CROUCH"),
    RIDING("RIDING");

    @lombok.Getter
    private final String tsIndex;
    PlayerMovementState(String tsIndex) { this.tsIndex = tsIndex; }
}
