/*
 * Source TS: PlayerMovementState.ts
 * Original TS: 'enum PlayerMovementState'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum PlayerMovementState {
    WALK(1),
    SPRINT(2),
    JUMP(3),
    FALL(4),
    FLY(5),
    SWIM(6),
    CROUCH(7),
    RIDING(8);

    @lombok.Getter
    private final int tsIndex;
    PlayerMovementState(int tsIndex) { this.tsIndex = tsIndex; }
}
