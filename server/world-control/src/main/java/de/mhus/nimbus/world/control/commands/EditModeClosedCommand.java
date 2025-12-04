package de.mhus.nimbus.world.control.commands;

import de.mhus.nimbus.world.shared.commands.Command;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Command to cleanup edit mode overlays when a session closes.
 * Called by world-player when:
 * 1. Session disconnects
 * 2. Edit mode is disabled
 *
 * Deletes all Redis keys matching: world:{worldId}:overlay:{sessionId}:*
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EditModeClosedCommand implements Command {

    private final WorldRedisService redisService;

    @Override
    public String getName() {
        return "EditModeClosed";
    }

    @Override
    public CommandResult execute(CommandContext context, List<String> args) {
        String worldId = context.getWorldId();
        String sessionId = context.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("EditModeClosed called without sessionId");
            return CommandResult.error(-3, "sessionId required");
        }

        try {
            // Delete all overlays for this session
            long deleted = redisService.deleteAllOverlays(worldId, sessionId);

            log.info("Cleaned up edit mode overlays: worldId={}, sessionId={}, keysDeleted={}",
                    worldId, sessionId, deleted);

            return CommandResult.success("Cleaned up " + deleted + " overlay keys");

        } catch (Exception e) {
            log.error("Failed to cleanup overlays: worldId={}, sessionId={}",
                    worldId, sessionId, e);
            return CommandResult.error(-4, "Cleanup failed: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() {
        return "Cleanup edit mode overlays for a session\n" +
                "Internal command - called automatically by world-player\n" +
                "Deletes all Redis overlay keys for the specified session";
    }
}
