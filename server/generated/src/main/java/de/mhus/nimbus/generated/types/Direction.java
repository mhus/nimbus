package de.mhus.nimbus.generated.types;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum Direction {
    NORTH(1 << 0),
    SOUTH(1 << 1),
    EAST(1 << 2),
    WEST(1 << 3),
    UP(1 << 4),
    DOWN(1 << 5);

    private final int value;

    Direction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
