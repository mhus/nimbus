/*
 * Source TS: EditAction.ts
 * Original TS: 'enum EditAction'
 */
package de.mhus.nimbus.generated.types;

public enum EditAction {
    OPEN_CONFIG_DIALOG("OPEN_CONFIG_DIALOG"),
    OPEN_EDITOR("OPEN_EDITOR"),
    MARK_BLOCK("MARK_BLOCK"),
    COPY_BLOCK("COPY_BLOCK"),
    DELETE_BLOCK("DELETE_BLOCK"),
    MOVE_BLOCK("MOVE_BLOCK");

    @lombok.Getter
    private final String tsIndex;
    EditAction(String tsIndex) { this.tsIndex = tsIndex; }
}
