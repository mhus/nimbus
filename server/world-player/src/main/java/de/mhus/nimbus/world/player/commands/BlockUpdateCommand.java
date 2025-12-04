package de.mhus.nimbus.world.player.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.commands.Command;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.List;
import java.util.Optional;

/**
 * BlockUpdate command - sends "b.u" message to client via WebSocket.
 * Triggered by world-control after block edit operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlockUpdateCommand implements Command {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "BlockUpdate";
    }

    @Override
    public CommandResult execute(CommandContext context, List<String> args) {
        // Args: [x, y, z, blockId, meta]
        if (args.size() != 5) {
            return CommandResult.error(-3, "Usage: BlockUpdate <x> <y> <z> <blockId> <meta>");
        }

        String sessionId = context.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return CommandResult.error(-2, "Session ID required");
        }

        try {
            int x = Integer.parseInt(args.get(0));
            int y = Integer.parseInt(args.get(1));
            int z = Integer.parseInt(args.get(2));
            String blockId = args.get(3);
            String meta = args.get(4);

            // Find session
            Optional<PlayerSession> sessionOpt = sessionManager.getBySessionId(sessionId);
            if (sessionOpt.isEmpty()) {
                return CommandResult.error(-4, "Session not found: " + sessionId);
            }

            PlayerSession session = sessionOpt.get();

            // Build "b.u" message
            NetworkMessage message = NetworkMessage.builder()
                    .t("b.u")
                    .d(objectMapper.valueToTree(List.of(x, y, z, blockId, meta.isEmpty() ? null : meta)))
                    .build();

            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            // Send via WebSocket
            session.getWebSocketSession().sendMessage(textMessage);

            log.debug("Sent block update to client: session={} pos=({},{},{}) blockId={}",
                    sessionId, x, y, z, blockId);

            return CommandResult.success("Block update sent to client");

        } catch (NumberFormatException e) {
            return CommandResult.error(-5, "Invalid coordinates: " + e.getMessage());
        } catch (Exception e) {
            log.error("BlockUpdate failed: session={}", sessionId, e);
            return CommandResult.error(-6, "Internal error: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() {
        return "Send block update to client via WebSocket (called by world-control)";
    }

    @Override
    public boolean requiresSession() {
        return false;  // sessionId in context
    }
}
