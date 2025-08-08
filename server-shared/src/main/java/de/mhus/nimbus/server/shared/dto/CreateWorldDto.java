package de.mhus.nimbus.server.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object for creating a new world.
 * Used in POST requests to create worlds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorldDto {

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
