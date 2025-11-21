/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldDetailDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double assetPort;
    private String worldGroupId;
    private String createdAt;
    private String updatedAt;
    private UserDTO owner;
    private WorldSettingsDTO settings;
}
