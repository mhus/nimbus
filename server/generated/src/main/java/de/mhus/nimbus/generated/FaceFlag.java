package de.mhus.nimbus.generated;

/**
 * Generated from Block.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum FaceFlag {
    TOP(1 << 0),
    0b00000001(1),
    BOTTOM(1 << 1),
    0b00000010(2),
    LEFT(1 << 2),
    0b00000100(4),
    RIGHT(1 << 3),
    0b00001000(8),
    FRONT(1 << 4),
    0b00010000(16),
    BACK(1 << 5),
    0b00100000(32),
    FIXED(1 << 6),
    0b01000000(64 (fixed mode);

    private final int value;

    FaceFlag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
