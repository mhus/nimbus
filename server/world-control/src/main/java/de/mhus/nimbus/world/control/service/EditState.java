package de.mhus.nimbus.world.control.service;

import de.mhus.nimbus.world.shared.layer.EditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Edit state for a session.
 * Stored in Redis for sharing across pods.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditState {

    /**
     * Edit mode enabled flag.
     */
    @Builder.Default
    private boolean editMode = false;

    /**
     * Current edit action (default: OPEN_CONFIG_DIALOG).
     */
    private EditAction editAction;

    /**
     * Selected layer name for editing (null = legacy mode, all layers).
     */
    private String selectedLayer;

    /**
     * Mount point X coordinate (for ModelLayer editing).
     */
    private Integer mountX;

    /**
     * Mount point Y coordinate (for ModelLayer editing).
     */
    private Integer mountY;

    /**
     * Mount point Z coordinate (for ModelLayer editing).
     */
    private Integer mountZ;

    /**
     * Selected group number (default: 0 = no group).
     */
    @Builder.Default
    private int selectedGroup = 0;

    /**
     * Last update timestamp.
     */
    private Instant lastUpdated;

    /**
     * World ID (for validation).
     */
    private String worldId;

}
