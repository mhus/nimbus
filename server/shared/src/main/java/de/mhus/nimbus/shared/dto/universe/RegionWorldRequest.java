package de.mhus.nimbus.shared.dto.universe;

/**
 * Request-DTO für Universe-Route /universe/region/{regionId}/world/{worldId}.
 */
public record RegionWorldRequest(
        String name,
        String description,
        String planetId,
        String solarSystemId,
        String galaxyId,
        String coordinates
) {}
