/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'enum WEARABLE_SLOT'
 */
package de.mhus.nimbus.generated.configs;

public enum WEARABLE_SLOT {
    HEAD(1),
    BODY(2),
    LEGS(3),
    FEET(4),
    HANDS(5),
    NECK(6),
    LEFT_RING(7),
    RIGHT_RING(8),
    LEFT_WEAPON_1(9),
    RIGHT_WEAPON_1(10),
    LEFT_WEAPON_2(11),
    RIGHT_WEAPON_2(12);

    @lombok.Getter
    private final int tsIndex;
    WEARABLE_SLOT(int tsIndex) { this.tsIndex = tsIndex; }
}
