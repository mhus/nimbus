/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum TextureKey'
 */
package de.mhus.nimbus.generated.types;

public enum TextureKey {
    ALL(1),
    TOP(2),
    BOTTOM(3),
    LEFT(4),
    RIGHT(5),
    FRONT(6),
    BACK(7),
    SIDE(8),
    DIFFUSE(9),
    DISTORTION(10),
    OPACITY(11),
    WALL(12),
    INSIDE_ALL(13),
    INSIDE_TOP(14),
    INSIDE_BOTTOM(15),
    INSIDE_LEFT(16),
    INSIDE_RIGHT(17),
    INSIDE_FRONT(18),
    INSIDE_BACK(19),
    INSIDE_SIDE(20);

    @lombok.Getter
    private final int tsIndex;
    TextureKey(int tsIndex) { this.tsIndex = tsIndex; }
}
