package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.commands.Command;
import de.mhus.nimbus.world.player.commands.CommandService;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles client command messages.
 * Message type: "cmd" (Client Command, Client → Server)
 *
 * Client sends commands for execution.
 * Server executes via CommandService and sends response.
 *
 * Expected data:
 * {
 *   "cmd": "help",
 *   "args": ["say"],  // optional
 *   "oneway": false   // optional, true = no response expected
 * }
 *
 * Server can send streaming messages during execution:
 * Response type: "cmd.msg" (streaming message)
 * {
 *   "message": "Processing..."
 * }
 *
 * Final response type: "cmd.rs" (command result)
 * {
 *   "rc": 0,  // return code (0=success, negative=system error, positive=command error)
 *   "message": "Command output"
 * }
 *
 * Return codes:
 * -1 = Command not found
 * -2 = Command not allowed (permission denied)
 * -3 = Invalid arguments
 * -4 = Internal error
 *  0 = OK / true
 *  1 = Error / false
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientCommandHandler implements MessageHandler {

    private final CommandService commandService;
    private final ObjectMapper objectMapper;

    @Override
    public String getMessageType() {
        return "cmd";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        if (!session.isAuthenticated()) {
            log.warn("Command from unauthenticated session: {}",
                    session.getWebSocketSession().getId());
            return;
        }

        JsonNode data = message.getD();
        String requestId = message.getI();

        // Extract command data
        String commandName = data.has("cmd") ? data.get("cmd").asText() : null;
        boolean oneway = data.has("oneway") && data.get("oneway").asBoolean();
        List<String> args = new ArrayList<>();

        if (data.has("args") && data.get("args").isArray()) {
            ArrayNode argsArray = (ArrayNode) data.get("args");
            for (JsonNode arg : argsArray) {
                args.add(arg.asText());
            }
        }

        if (commandName == null || commandName.isBlank()) {
            log.warn("Command without name");
            if (!oneway && requestId != null) {
                sendErrorResponse(session, requestId, -3, "Missing command name");
            }
            return;
        }

        // TODO: Check permissions (player allowed to execute this command?)

        // Execute command
        Command.CommandResult result = commandService.execute(session, commandName, args);

        // Send streaming messages if any
        if (result.getStreamMessages() != null && !result.getStreamMessages().isEmpty()) {
            for (String streamMessage : result.getStreamMessages()) {
                sendStreamingMessage(session, requestId, streamMessage);
            }
        }

        // Send final response (unless oneway)
        if (!oneway && requestId != null) {
            sendResponse(session, requestId, result.getReturnCode(), result.getMessage());
        }

        log.debug("Command executed: cmd={}, rc={}, user={}, session={}",
                commandName, result.getReturnCode(), session.getDisplayName(), session.getSessionId());
    }

    /**
     * Send streaming message during command execution.
     */
    private void sendStreamingMessage(PlayerSession session, String requestId, String message) {
        try {
            ObjectNode data = objectMapper.createObjectNode();
            data.put("message", message);

            NetworkMessage response = NetworkMessage.builder()
                    .r(requestId)
                    .t("cmd.msg")
                    .d(data)
                    .build();

            String json = objectMapper.writeValueAsString(response);
            session.getWebSocketSession().sendMessage(new TextMessage(json));

            log.trace("Sent command streaming message: requestId={}", requestId);

        } catch (Exception e) {
            log.error("Failed to send command streaming message", e);
        }
    }

    /**
     * Send final command response.
     */
    private void sendResponse(PlayerSession session, String requestId, int returnCode, String message) {
        try {
            ObjectNode data = objectMapper.createObjectNode();
            data.put("rc", returnCode);
            if (message != null) {
                data.put("message", message);
            }

            NetworkMessage response = NetworkMessage.builder()
                    .r(requestId)
                    .t("cmd.rs")
                    .d(data)
                    .build();

            String json = objectMapper.writeValueAsString(response);
            session.getWebSocketSession().sendMessage(new TextMessage(json));

            log.trace("Sent command response: requestId={}, rc={}", requestId, returnCode);

        } catch (Exception e) {
            log.error("Failed to send command response", e);
        }
    }

    /**
     * Send error response for invalid requests.
     */
    private void sendErrorResponse(PlayerSession session, String requestId, int errorCode, String errorMessage) {
        sendResponse(session, requestId, errorCode, errorMessage);
    }
}
