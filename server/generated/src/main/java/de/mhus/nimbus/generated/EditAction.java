package de.mhus.nimbus.generated;

/**
 * Generated from EditAction.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum EditAction {
    OPEN_CONFIG_DIALOG('OPEN_CONFIG_DIALOG'),
    OPEN_EDITOR('OPEN_EDITOR'),
    MARK_BLOCK('MARK_BLOCK'),
    COPY_BLOCK('COPY_BLOCK'),
    DELETE_BLOCK('DELETE_BLOCK'),
    MOVE_BLOCK('MOVE_BLOCK');

    private final int value;

    EditAction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
