package de.mhus.nimbus.world.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified world information DTO for list operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldInfoDto {

    private String worldId;
    private String name;
    private String description;
    private String regionId;
    private boolean enabled;
    private boolean publicFlag;
}
