package de.mhus.nimbus.region.universe;

/**
 * Request-DTO für die Universe-Route /universe/region/{regionId}/world/{worldId}.
 */
public record RegionWorldRequest(
        String name,
        String description,
        String planetId,
        String solarSystemId,
        String galaxyId,
        String coordinates
) {}
