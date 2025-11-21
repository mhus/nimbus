package de.mhus.nimbus.shared.dto.region;

/**
 * Request-DTO für Region-Route /region/{regionId}/world/{worldId}.
 */
public record RegionWorldRequest(
        String name,
        String description,
        String worldApiUrl
) {}
