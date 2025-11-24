/*
 * Source TS: Modal.ts
 * Original TS: 'enum ModalSizePreset'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum ModalSizePreset {
    LEFT(1),
    RIGHT(2),
    TOP(3),
    BOTTOM(4),
    CENTER_SMALL(5),
    CENTER_MEDIUM(6),
    CENTER_LARGE(7),
    LEFT_TOP(8),
    LEFT_BOTTOM(9),
    RIGHT_TOP(10),
    RIGHT_BOTTOM(11);

    @lombok.Getter
    private final int tsIndex;
    ModalSizePreset(int tsIndex) { this.tsIndex = tsIndex; }
}
