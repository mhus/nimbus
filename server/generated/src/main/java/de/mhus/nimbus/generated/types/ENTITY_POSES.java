/*
 * Source TS: EntityData.ts
 * Original TS: 'enum ENTITY_POSES'
 */
package de.mhus.nimbus.generated.types;

public enum ENTITY_POSES {
    IDLE(0),
    WALK(1),
    RUN(2),
    SPRINT(3),
    CROUCH(4),
    JUMP(5),
    SWIM(6),
    FLY(7),
    DEATH(8),
    WALK_SLOW(9),
    CLAPPING(10),
    ROLL(11),
    ATTACK(12),
    OUT_OF_WATER(13),
    SWIMMING_FAST(14),
    SWIMMING_IMPULSIVE(15),
    SWIMMING(16),
    HIT_RECEIVED(17),
    HIT_RECEIVED_STRONG(18),
    KICK_LEFT(19),
    KICK_RIGHT(20),
    PUNCH_LEFT(21),
    PUNCH_RIGHT(22),
    RUN_BACKWARD(23),
    RUN_LEFT(24),
    RUN_RIGHT(25),
    WAVE(26);

    @lombok.Getter
    private final int tsIndex;
    ENTITY_POSES(int tsIndex) { this.tsIndex = tsIndex; }
}
