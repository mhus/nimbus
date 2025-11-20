/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldDetailDTO'
 */
package de.mhus.nimbus.generated.rest;

@lombok.Data
@lombok.Builder
public class WorldDetailDTO extends Object {
    private String worldId;
    private String name;
    private String description;
    private Position3D start;
    private Position3D stop;
    private double chunkSize;
    private String assetPath;
    private java.lang.Double assetPort;
    private String worldGroupId;
    private String createdAt;
    private String updatedAt;
    private UserDTO owner;
    private WorldSettingsDTO settings;
}
