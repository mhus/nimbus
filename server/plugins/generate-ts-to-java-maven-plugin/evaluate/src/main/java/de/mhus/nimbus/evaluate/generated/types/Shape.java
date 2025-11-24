/*
 * Source TS: Shape.ts
 * Original TS: 'enum Shape'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum Shape {
    INVISIBLE(1),
    CUBE(2),
    CROSS(3),
    HASH(4),
    MODEL(5),
    GLASS(6),
    GLASS_FLAT(7),
    FLAT(8),
    SPHERE(9),
    CYLINDER(10),
    ROUND_CUBE(11),
    STEPS(12),
    STAIR(13),
    BILLBOARD(14),
    SPRITE(15),
    FLAME(16),
    OCEAN(17),
    OCEAN_COAST(18),
    OCEAN_MAELSTROM(19),
    RIVER(20),
    RIVER_WATERFALL(21),
    RIVER_WATERFALL_WHIRLPOOL(22),
    WATER(23),
    LAVA(24),
    FOG(25),
    THIN_INSTANCES(26),
    WALL(27),
    FLIPBOX(28),
    ITEM(29);

    @lombok.Getter
    private final int tsIndex;
    Shape(int tsIndex) { this.tsIndex = tsIndex; }
}
