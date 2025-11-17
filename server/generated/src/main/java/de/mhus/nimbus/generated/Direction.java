package de.mhus.nimbus.generated;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum Direction {
    NORTH(1 << 0),
    0b000001(1),
    SOUTH(1 << 1),
    0b000010(2),
    EAST(1 << 2),
    0b000100(4),
    WEST(1 << 3),
    0b001000(8),
    UP(1 << 4),
    0b010000(16),
    DOWN(1 << 5),
    0b100000(32);

    private final int value;

    Direction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
