/*
 * Source TS: AnimationData.ts
 * Original TS: 'enum EasingType'
 */
package de.mhus.nimbus.generated.types;

public enum EasingType {
    LINEAR(1),
    EASE_IN(2),
    EASE_OUT(3),
    EASE_IN_OUT(4),
    ELASTIC(5),
    BOUNCE(6),
    STEP(7);

    @lombok.Getter
    private final int tsIndex;
    EasingType(int tsIndex) { this.tsIndex = tsIndex; }
}
