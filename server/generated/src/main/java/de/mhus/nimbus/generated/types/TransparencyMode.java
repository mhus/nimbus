/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum TransparencyMode'
 */
package de.mhus.nimbus.generated.types;

public enum TransparencyMode {
    NONE(0),
    ALPHA_TEST(1),
    ALPHA_BLEND(2),
    ALPHA_TEST_FROM_RGB(3),
    ALPHA_BLEND_FROM_RGB(4),
    ALPHA_TESTANDBLEND(5),
    ALPHA_TESTANDBLEND_FROM_RGB(6);

    @lombok.Getter
    private final int tsIndex;
    TransparencyMode(int tsIndex) { this.tsIndex = tsIndex; }
}
