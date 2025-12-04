package de.mhus.nimbus.world.player.ws.commands;

import de.mhus.nimbus.world.player.service.EditModeService;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.commands.Command;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Command to enable/disable/query edit mode for a session.
 *
 * Usage:
 *   /edit          - Query current edit mode status
 *   /edit true     - Enable edit mode
 *   /edit false    - Disable edit mode
 *
 * Requires active WebSocket session.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEditCommand implements Command {

    private final SessionManager sessionManager;
    private final EditModeService editModeService;

    @Override
    public String getName() {
        return "edit";
    }

    @Override
    public boolean requiresSession() {
        return true;
    }

    @Override
    public CommandResult execute(CommandContext context, List<String> args) {
        // Get session
        PlayerSession session = sessionManager.getBySessionId(context.getSessionId())
                .orElse(null);

        if (session == null) {
            log.warn("Session not found: sessionId={}", context.getSessionId());
            return CommandResult.error(-2, "Session not found");
        }

        // No arguments = query status
        if (args == null || args.isEmpty()) {
            boolean editMode = editModeService.isEditMode(session);
            return CommandResult.success("Edit mode: " + editMode);
        }

        // Parse boolean argument
        String arg = args.get(0).toLowerCase();
        boolean enable = "true".equals(arg) || "1".equals(arg) || "on".equals(arg);
        boolean disable = "false".equals(arg) || "0".equals(arg) || "off".equals(arg);

        if (!enable && !disable) {
            return CommandResult.error(-3, "Invalid argument. Use 'true' or 'false'");
        }

        // Enable or disable
        if (enable) {
            editModeService.enableEditMode(session);
            return CommandResult.success("Edit mode enabled");
        } else {
            editModeService.disableEditMode(session);
            return CommandResult.success("Edit mode disabled");
        }
    }

    @Override
    public String getHelp() {
        return "Enable/disable/query edit mode for your session\n" +
                "Usage:\n" +
                "  /edit          - Query current edit mode status\n" +
                "  /edit true     - Enable edit mode\n" +
                "  /edit false    - Disable edit mode\n" +
                "\n" +
                "When edit mode is enabled, you see overlay blocks from Redis.\n" +
                "Overlays are stored per-session and automatically cleaned up on disconnect.\n" +
                "\n" +
                "Examples:\n" +
                "  /edit\n" +
                "  /edit true\n" +
                "  /edit false";
    }
}
