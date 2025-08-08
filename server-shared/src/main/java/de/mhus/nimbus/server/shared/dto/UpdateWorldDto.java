package de.mhus.nimbus.server.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object for updating an existing world.
 * Used in PUT requests to update world information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorldDto {

    /**
     * Name of the world
     */
    private String name;

    /**
     * Description of the world
     */
    private String description;

    /**
     * URL for accessing the world's WebSocket endpoint
     */
    private String accessUrl;

    /**
     * Additional properties of the world
     */
    private Map<String, String> properties;
}
