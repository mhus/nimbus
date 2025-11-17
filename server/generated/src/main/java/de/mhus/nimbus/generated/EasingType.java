package de.mhus.nimbus.generated;

/**
 * Generated from AnimationData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum EasingType {
    LINEAR("linear"),
    EASE_IN("easeIn"),
    EASE_OUT("easeOut"),
    EASE_IN_OUT("easeInOut"),
    ELASTIC("elastic"),
    BOUNCE("bounce"),
    STEP("step");

    private final String value;

    EasingType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
