package de.mhus.nimbus.generated;

/**
 * Generated from Modal.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum ModalFlags {
    NONE(0),
    CLOSEABLE(1 << 0),
    NO_BORDERS(1 << 1),
    BREAK_OUT(1 << 2),
    NO_BACKGROUND_LOCK(1 << 3),
    MOVEABLE(1 << 4),
    RESIZEABLE(1 << 5);

    private final int value;

    ModalFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
