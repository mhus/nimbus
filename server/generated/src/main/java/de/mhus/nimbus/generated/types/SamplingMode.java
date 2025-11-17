package de.mhus.nimbus.generated.types;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum SamplingMode {
    NEAREST(0),
    LINEAR(1),
    MIPMAP(2);

    private final int value;

    SamplingMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
