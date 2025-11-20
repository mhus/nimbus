/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldListItemDTO'
 */
package de.mhus.nimbus.generated.rest;

@lombok.Data
@lombok.Builder
public class WorldListItemDTO extends Object {
    private String worldId;
    private String name;
    private String description;
    private UserDTO owner;
    private String createdAt;
    private String updatedAt;
}
