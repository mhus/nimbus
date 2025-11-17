package de.mhus.nimbus.generated;

/**
 * Generated from Modal.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum ModalSizePreset {
    LEFT('left'),
    RIGHT('right'),
    TOP('top'),
    BOTTOM('bottom'),
    CENTER_SMALL('center_small'),
    CENTER_MEDIUM('center_medium'),
    CENTER_LARGE('center_large'),
    LEFT_TOP('left_top'),
    LEFT_BOTTOM('left_bottom'),
    RIGHT_TOP('right_top'),
    RIGHT_BOTTOM('right_bottom');

    private final int value;

    ModalSizePreset(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
