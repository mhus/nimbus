package de.mhus.nimbus.world.shared.dto;

import de.mhus.nimbus.shared.annotations.GenerateTypeScript;
import de.mhus.nimbus.shared.annotations.TypeScript;
import de.mhus.nimbus.world.shared.layer.LayerType;

import java.time.Instant;
import java.util.List;

/**
 * DTO for WLayer entity.
 * Used for API responses and TypeScript generation.
 *
 * Note: mountX/Y/Z and groups are now in WLayerModel, not WLayer.
 */
@GenerateTypeScript("dto")
public record LayerDto(
        @TypeScript(optional = true)
        String id,

        String worldId,

        String name,

        @TypeScript(follow = true)
        LayerType layerType,

        @TypeScript(optional = true)
        String layerDataId,

        boolean allChunks,

        @TypeScript(optional = true)
        List<String> affectedChunks,

        int order,

        boolean enabled,

        @TypeScript(optional = true)
        Instant createdAt,

        @TypeScript(optional = true)
        Instant updatedAt
) {
}
