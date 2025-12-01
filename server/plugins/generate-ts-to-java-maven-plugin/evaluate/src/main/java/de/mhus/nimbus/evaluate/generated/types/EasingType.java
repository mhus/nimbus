/*
 * Source TS: AnimationData.ts
 * Original TS: 'enum EasingType'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum EasingType {
    LINEAR("linear"),
    EASE_IN("easeIn"),
    EASE_OUT("easeOut"),
    EASE_IN_OUT("easeInOut"),
    ELASTIC("elastic"),
    BOUNCE("bounce"),
    STEP("step");

    @lombok.Getter
    private final String tsIndex;
    EasingType(String tsIndex) { this.tsIndex = tsIndex; }
}
