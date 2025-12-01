/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldListItemDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldListItemDTO {
    @com.fasterxml.jackson.annotation.JsonProperty("worldId")
    private String worldId;
    private String name;
    private String description;
    private UserDTO owner;
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    private String createdAt;
    @com.fasterxml.jackson.annotation.JsonProperty("updatedAt")
    private String updatedAt;
}
