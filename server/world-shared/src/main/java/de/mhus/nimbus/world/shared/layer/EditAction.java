package de.mhus.nimbus.world.shared.layer;

/**
 * Edit action types for block editing workflow.
 * Replaces deprecated generated/types/EditAction.
 */
public enum EditAction {
    /**
     * Opens the config dialog for the block (default action).
     */
    OPEN_CONFIG_DIALOG,

    /**
     * Opens the block editor at the selected position.
     */
    OPEN_EDITOR,

    /**
     * Marks the block for copy/move operations.
     */
    MARK_BLOCK,

    /**
     * Copies the marked block to the selected position.
     */
    COPY_BLOCK,

    /**
     * Deletes the block at the selected position.
     */
    DELETE_BLOCK,

    /**
     * Moves the marked block to the selected position.
     */
    MOVE_BLOCK
}
