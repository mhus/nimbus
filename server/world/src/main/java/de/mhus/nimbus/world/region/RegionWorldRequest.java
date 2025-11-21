package de.mhus.nimbus.world.region;

/**
 * Request-DTO für die Region-Route /region/{regionId}/world/{worldId}.
 */
public record RegionWorldRequest(
        String name,
        String description,
        String worldApiUrl
) {}
