package de.mhus.nimbus.generated;

/**
 * Generated from EntityData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
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

    private final int value;

    ENTITY_POSES(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
