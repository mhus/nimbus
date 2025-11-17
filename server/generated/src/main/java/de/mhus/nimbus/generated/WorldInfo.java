package de.mhus.nimbus.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from World.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldInfo {

    /**
     * worldId
     */
    private String worldId;

    /**
     * name
     */
    private String name;

    /**
     * description (optional)
     */
    private String description;

    /**
     * start (optional)
     */
    private Vector3 start;

    /**
     * stop (optional)
     */
    private Vector3 stop;

    /**
     * chunkSize (optional)
     */
    private double chunkSize;

    /**
     * assetPath (optional)
     */
    private String assetPath;

    /**
     * assetPort (optional)
     */
    private double assetPort;

    /**
     * worldGroupId (optional)
     */
    private String worldGroupId;

    /**
     * status (optional)
     */
    private double status;

    /**
     * createdAt (optional)
     */
    private String createdAt;

    /**
     * updatedAt (optional)
     */
    private String updatedAt;

    /**
     * owner (optional)
     */
    private { owner;

    /**
     * user
     */
    private String user;

    /**
     * displayName
     */
    private String displayName;

    /**
     * email (optional)
     */
    private String email;
}
