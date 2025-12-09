package de.mhus.nimbus.world.player.ws.dto;

import de.mhus.nimbus.generated.types.EntityPathway;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Container for EntityPathway with metadata for distribution.
 * Used for Redis messages to enable proper filtering and routing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PathwayContainer {

    /**
     * The entity pathway data.
     */
    private EntityPathway pathway;

    /**
     * Session ID that generated this pathway.
     * Used to prevent echoing pathways back to the originating session.
     */
    private String sessionId;

    /**
     * World ID where this pathway belongs.
     * Redundant but useful for validation.
     */
    private String worldId;
}
