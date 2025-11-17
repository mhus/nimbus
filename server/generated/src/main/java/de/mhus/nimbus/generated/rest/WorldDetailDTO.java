package de.mhus.nimbus.generated.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from WorldDTO.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldDetailDTO {

    /**
     * worldId
     */
    private String worldId;

    /**
     * name
     */
    private String name;

    /**
     * description
     */
    private String description;

    /**
     * start
     */
    private Position3D start;

    /**
     * stop
     */
    private Position3D stop;

    /**
     * chunkSize
     */
    private double chunkSize;

    /**
     * assetPath
     */
    private String assetPath;

    /**
     * assetPort (optional)
     */
    private double assetPort;

    /**
     * worldGroupId
     */
    private String worldGroupId;

    /**
     * createdAt
     */
    private String createdAt;

    /**
     * updatedAt
     */
    private String updatedAt;

    /**
     * owner
     */
    private UserDTO owner;

    /**
     * settings
     */
    private WorldSettingsDTO settings;
}
