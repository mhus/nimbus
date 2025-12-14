package de.mhus.nimbus.world.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified character information DTO for list operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterInfoDto {

    private String id;
    private String name;
    private String display;
    private String userId;
    private String regionId;
}
