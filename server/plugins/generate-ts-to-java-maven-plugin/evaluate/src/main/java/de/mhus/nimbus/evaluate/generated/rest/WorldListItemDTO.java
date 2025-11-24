/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldListItemDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldListItemDTO {
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
    private UserDTO owner;
    @Deprecated
    @SuppressWarnings("required")
    private String createdAt;
    @Deprecated
    @SuppressWarnings("required")
    private String updatedAt;
}
