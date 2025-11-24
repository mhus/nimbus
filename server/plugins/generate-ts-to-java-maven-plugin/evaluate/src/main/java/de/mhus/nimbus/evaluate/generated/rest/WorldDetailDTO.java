/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldDetailDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldDetailDTO {
    @Deprecated
    @SuppressWarnings("required")
    private String worldId;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("required")
    private String description;
    @Deprecated
    @SuppressWarnings("required")
    private Position3D start;
    @Deprecated
    @SuppressWarnings("required")
    private Position3D stop;
    @Deprecated
    @SuppressWarnings("required")
    private double chunkSize;
    @Deprecated
    @SuppressWarnings("required")
    private String assetPath;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double assetPort;
    @Deprecated
    @SuppressWarnings("required")
    private String worldGroupId;
    @Deprecated
    @SuppressWarnings("required")
    private String createdAt;
    @Deprecated
    @SuppressWarnings("required")
    private String updatedAt;
    @Deprecated
    @SuppressWarnings("required")
    private UserDTO owner;
    @Deprecated
    @SuppressWarnings("required")
    private WorldSettingsDTO settings;
}
