/*
 * Source TS: Modal.ts
 * Original TS: 'enum ModalFlags'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum ModalFlags {
    NONE(1),
    CLOSEABLE(2),
    NO_BORDERS(3),
    BREAK_OUT(4),
    NO_BACKGROUND_LOCK(5),
    MOVEABLE(6),
    RESIZEABLE(7);

    @lombok.Getter
    private final int tsIndex;
    ModalFlags(int tsIndex) { this.tsIndex = tsIndex; }
}
