package de.mhus.nimbus.shared.dto.region;

import java.util.Date;

/**
 * Response-DTO für Region-Route /region/{regionId}/world/{worldId}.
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
