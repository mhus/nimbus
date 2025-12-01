/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'enum PlayerMovementState'
 */
package de.mhus.nimbus.generated.types;

public enum PlayerMovementState {
    WALK(1),
    SPRINT(2),
    JUMP(3),
    FALL(4),
    FREE_FLY(5),
    FLY(6),
    SWIM(7),
    CROUCH(8),
    RIDING(9);

    @lombok.Getter
    private final int tsIndex;
    PlayerMovementState(int tsIndex) { this.tsIndex = tsIndex; }
}
