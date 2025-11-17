package de.mhus.nimbus.generated.types;

/**
 * Generated from BlockType.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum BlockStatus {
    DEFAULT(0),
    OPEN(1),
    CLOSED(2),
    LOCKED(3),
    DESTROYED(5),
    WINTER(10),
    WINTER_SPRING(11),
    SPRING(12),
    SPRING_SUMMER(13),
    SUMMER(14),
    SUMMER_AUTUMN(15),
    AUTUMN(16),
    AUTUMN_WINTER(17),
    CUSTOM_START(100);

    private final int value;

    BlockStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
