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
public class WorldListItemDTO {

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
     * owner
     */
    private UserDTO owner;

    /**
     * createdAt
     */
    private String createdAt;

    /**
     * updatedAt
     */
    private String updatedAt;
}
