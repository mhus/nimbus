/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum TransparencyMode'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum TransparencyMode {
    NONE(1),
    ALPHA_TEST(2),
    ALPHA_BLEND(3),
    ALPHA_TEST_FROM_RGB(4),
    ALPHA_BLEND_FROM_RGB(5),
    ALPHA_TESTANDBLEND(6),
    ALPHA_TESTANDBLEND_FROM_RGB(7);

    @lombok.Getter
    private final int tsIndex;
    TransparencyMode(int tsIndex) { this.tsIndex = tsIndex; }
}
