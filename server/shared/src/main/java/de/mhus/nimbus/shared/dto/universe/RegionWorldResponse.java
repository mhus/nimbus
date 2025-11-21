package de.mhus.nimbus.shared.dto.universe;

import java.util.Date;

/**
 * Response-DTO für Universe-Route /universe/region/{regionId}/world/{worldId}.
 */
public record RegionWorldResponse(
        String id,
        String name,
        String description,
        Date createdAt,
        String regionId,
        String planetId,
        String solarSystemId,
        String galaxyId,
        String worldId,
        String coordinates
) {}
