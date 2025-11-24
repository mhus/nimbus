/*
 * Source TS: EntityData.ts
 * Original TS: 'enum ENTITY_POSES'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum ENTITY_POSES {
    IDLE(1),
    WALK(2),
    RUN(3),
    SPRINT(4),
    CROUCH(5),
    JUMP(6),
    SWIM(7),
    FLY(8),
    DEATH(9),
    WALK_SLOW(10),
    CLAPPING(11),
    ROLL(12),
    ATTACK(13),
    OUT_OF_WATER(14),
    SWIMMING_FAST(15),
    SWIMMING_IMPULSIVE(16),
    SWIMMING(17),
    HIT_RECEIVED(18),
    HIT_RECEIVED_STRONG(19),
    KICK_LEFT(20),
    KICK_RIGHT(21),
    PUNCH_LEFT(22),
    PUNCH_RIGHT(23),
    RUN_BACKWARD(24),
    RUN_LEFT(25),
    RUN_RIGHT(26),
    WAVE(27);

    @lombok.Getter
    private final int tsIndex;
    ENTITY_POSES(int tsIndex) { this.tsIndex = tsIndex; }
}
