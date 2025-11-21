package de.mhus.nimbus.world.region;

import java.util.Date;

/**
 * Response-DTO spiegelbildlich zur Region-Antwort auf
 * /region/{regionId}/world/{worldId}.
 */
public record RegionWorldResponse(
        String id,
        String worldId,
        String name,
        String description,
        String worldApiUrl,
        String regionId,
        Date createdAt
) {}
