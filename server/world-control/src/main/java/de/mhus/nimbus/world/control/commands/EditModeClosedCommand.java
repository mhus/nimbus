package de.mhus.nimbus.world.control.commands;

import de.mhus.nimbus.world.shared.commands.Command;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Command to handle edit mode closure with auto-save functionality.
 * Called by world-player when:
 * 1. Session disconnects
 * 2. Edit mode is disabled
 *
 * AUTO-SAVES overlays to layer before cleanup (instead of discarding).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EditModeClosedCommand implements Command {

    private final CommitLayerCommand commitCommand;
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
            // AUTO-SAVE: Commit overlays to layer before closing
            log.info("Auto-saving overlays before session close: worldId={}, sessionId={}",
                    worldId, sessionId);

            // Delegate to CommitLayerCommand (synchronous execution)
            CommandResult commitResult = commitCommand.execute(context, List.of());

            if (commitResult.getReturnCode() == 0) {
                log.info("Auto-save successful: {}", commitResult.getMessage());
                return CommandResult.success("Auto-saved: " + commitResult.getMessage());
            } else {
                log.warn("Auto-save completed with warnings: {}", commitResult.getMessage());
                return CommandResult.success("Auto-saved with warnings: " + commitResult.getMessage());
            }

        } catch (Exception e) {
            log.error("Auto-save failed for worldId={}, sessionId={}: {}",
                    worldId, sessionId, e.getMessage(), e);

            // Fallback: Discard on error to prevent orphaned overlays in Redis
            try {
                long deleted = redisService.deleteAllOverlays(worldId, sessionId);
                log.warn("Auto-save failed, discarded {} overlay keys as fallback", deleted);
                return CommandResult.error(-4, "Auto-save failed, overlays discarded (" + deleted + " keys)");
            } catch (Exception discardError) {
                log.error("Fallback discard also failed: {}", discardError.getMessage());
                return CommandResult.error(-5, "Auto-save and discard both failed");
            }
        }
    }

    @Override
    public String getHelp() {
        return "Auto-save edit mode overlays to layer on session close\n" +
                "Internal command - called automatically by world-player\n" +
                "Commits all overlay blocks to the selected layer, then cleans up Redis";
    }
}
