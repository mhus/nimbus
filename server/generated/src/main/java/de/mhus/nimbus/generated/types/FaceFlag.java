package de.mhus.nimbus.generated.types;

/**
 * Generated from Block.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum FaceFlag {
    TOP(1 << 0),
    BOTTOM(1 << 1),
    LEFT(1 << 2),
    RIGHT(1 << 3),
    FRONT(1 << 4),
    BACK(1 << 5),
    FIXED(1 << 6);

    private final int value;

    FaceFlag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
