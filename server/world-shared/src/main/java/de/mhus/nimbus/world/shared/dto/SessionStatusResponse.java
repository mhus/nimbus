package de.mhus.nimbus.world.shared.dto;

import lombok.Builder;

import java.util.List;

/**
 * Response DTO for session status endpoint.
 * Returns current session information and logout URLs.
 */
@Builder
public record SessionStatusResponse(
        boolean authenticated,
        boolean agent,
        String worldId,
        String userId,
        String characterId,
        String actor,              // Actor role: PLAYER, EDITOR, SUPPORT (session only)
        List<String> roles,        // User roles: SECTOR_ADMIN, REGION_ADMIN, WORLD_OWNER, etc.
        String sessionId,
        List<String> accessUrls,
        String loginUrl,
        String logoutUrl
) {
}
