package de.mhus.nimbus.world.control.commands;

import de.mhus.nimbus.world.control.service.EditService;
import de.mhus.nimbus.world.control.service.EditState;
import de.mhus.nimbus.world.shared.commands.Command;
import de.mhus.nimbus.world.shared.commands.Command.CommandResult;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CommitLayer command - commits overlay edits to layer storage.
 * Reads overlays from Redis and saves them to the selected layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommitLayerCommand implements Command {

    private final EditService editService;
    private final WLayerService layerService;
    private final WorldRedisService redisService;

    @Override
    public String getName() {
        return "CommitLayer";
    }

    @Override
    public CommandResult execute(CommandContext context, List<String> args) {
        String sessionId = context.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return CommandResult.error(-2, "Session ID required");
        }

        // Get edit state to know which layer is being edited
        EditState state = editService.getEditState(context.getWorldId(), sessionId);

        if (state.getSelectedLayer() == null) {
            return CommandResult.error(-1, "No layer selected. Cannot commit without layer selection.");
        }

        if (!state.isEditMode()) {
            return CommandResult.error(-1, "Edit mode not active");
        }

        // TODO: Implement commit logic in Phase 2:
        // 1. Read overlays from Redis: world:{worldId}:overlay:{sessionId}:*
        // 2. Group by chunk
        // 3. For each chunk with overlays:
        //    a. Load existing LayerChunkData for this layer
        //    b. Apply overlays
        //    c. Save via WLayerService.saveTerrainChunk()
        //    d. Mark chunk dirty via WDirtyChunkService
        // 4. Clear overlays from Redis
        // 5. Return count of committed chunks

        log.info("CommitLayer called: session={} layer={} (not yet implemented)",
                sessionId, state.getSelectedLayer());

        return CommandResult.success("Layer commit placeholder (to be implemented)");
    }

    @Override
    public String getHelp() {
        return "Commit overlay edits to the selected layer";
    }

    @Override
    public boolean requiresSession() {
        return false;  // sessionId in context
    }
}
