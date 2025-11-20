/*
 * Source TS: EditAction.ts
 * Original TS: 'enum EditAction'
 */
package de.mhus.nimbus.generated.types;

public enum EditAction {
    OPEN_CONFIG_DIALOG(1),
    OPEN_EDITOR(2),
    MARK_BLOCK(3),
    COPY_BLOCK(4),
    DELETE_BLOCK(5),
    MOVE_BLOCK(6);

    @lombok.Getter
    private final int tsIndex;
    EditAction(int tsIndex) { this.tsIndex = tsIndex; }
}
