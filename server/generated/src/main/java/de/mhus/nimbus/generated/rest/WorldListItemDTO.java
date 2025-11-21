/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldListItemDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldListItemDTO {
    private String worldId;
    private String name;
    private String description;
    private UserDTO owner;
    private String createdAt;
    private String updatedAt;
}
