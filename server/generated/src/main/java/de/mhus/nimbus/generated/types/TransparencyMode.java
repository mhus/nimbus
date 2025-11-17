package de.mhus.nimbus.generated.types;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum TransparencyMode {
    NONE(0),
    ALPHA_TEST(1),
    ALPHA_BLEND(2),
    ALPHA_TEST_FROM_RGB(3),
    ALPHA_BLEND_FROM_RGB(4),
    ALPHA_TESTANDBLEND(5),
    ALPHA_TESTANDBLEND_FROM_RGB(6);

    private final int value;

    TransparencyMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
