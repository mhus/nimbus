/*
 * Source TS: Modal.ts
 * Original TS: 'enum ModalFlags'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum ModalFlags {
    NONE("0"),
    CLOSEABLE("1 << 0"),
    NO_BORDERS("1 << 1"),
    BREAK_OUT("1 << 2"),
    NO_BACKGROUND_LOCK("1 << 3"),
    MOVEABLE("1 << 4"),
    RESIZEABLE("1 << 5");

    @lombok.Getter
    private final String tsIndex;
    ModalFlags(String tsIndex) { this.tsIndex = tsIndex; }
}
