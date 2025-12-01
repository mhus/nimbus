/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'enum WEARABLE_SLOT'
 */
package de.mhus.nimbus.generated.configs;

public enum WEARABLE_SLOT {
    HEAD(0),
    BODY(1),
    LEGS(2),
    FEET(3),
    HANDS(4),
    NECK(5),
    LEFT_RING(6),
    RIGHT_RING(7),
    LEFT_WEAPON_1(8),
    RIGHT_WEAPON_1(9),
    LEFT_WEAPON_2(10),
    RIGHT_WEAPON_2(11);

    @lombok.Getter
    private final int tsIndex;
    WEARABLE_SLOT(int tsIndex) { this.tsIndex = tsIndex; }
}
