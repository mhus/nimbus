package de.mhus.nimbus.generated;

/**
 * Generated from PlayerMovementState.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum PlayerMovementState {
    WALK('WALK'),
    SPRINT('SPRINT'),
    JUMP('JUMP'),
    FALL('FALL'),
    FLY('FLY'),
    SWIM('SWIM'),
    CROUCH('CROUCH'),
    RIDING('RIDING');

    private final int value;

    PlayerMovementState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
