/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldDetailDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldDetailDTO {
    @com.fasterxml.jackson.annotation.JsonProperty("worldId")
    private String worldId;
    private String name;
    private String description;
    private Position3D start;
    private Position3D stop;
    @com.fasterxml.jackson.annotation.JsonProperty("chunkSize")
    private double chunkSize;
    @com.fasterxml.jackson.annotation.JsonProperty("assetPath")
    private String assetPath;
    @com.fasterxml.jackson.annotation.JsonProperty("assetPort")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double assetPort;
    @com.fasterxml.jackson.annotation.JsonProperty("worldGroupId")
    private String worldGroupId;
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    private String createdAt;
    @com.fasterxml.jackson.annotation.JsonProperty("updatedAt")
    private String updatedAt;
    private UserDTO owner;
    private WorldSettingsDTO settings;
}
