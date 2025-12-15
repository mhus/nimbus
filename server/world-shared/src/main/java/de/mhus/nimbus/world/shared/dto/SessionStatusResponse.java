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
        String role,
        String sessionId,
        List<String> accessUrls,
        String loginUrl
) {
}
