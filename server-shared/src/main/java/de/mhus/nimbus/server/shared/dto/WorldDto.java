package de.mhus.nimbus.server.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object for World information.
 * Used for API requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldDto {

    /**
     * Unique identifier of the world
     */
    private String id;

    /**
     * Name of the world
     */
    private String name;

    /**
     * Description of the world
     */
    private String description;

    /**
     * Creation timestamp in Unix time (milliseconds)
     */
    private Long createdAt;

    /**
     * Last update timestamp in Unix time (milliseconds)
     */
    private Long updatedAt;

    /**
     * Owner's user ID
     */
    private String ownerId;

    /**
     * Whether the world is enabled/active
     */
    private Boolean enabled;

    /**
     * URL for accessing the world's WebSocket endpoint
     */
    private String accessUrl;

    /**
     * Additional properties of the world
     */
    private Map<String, String> properties;
}
