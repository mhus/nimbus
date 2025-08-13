package de.mhus.nimbus.worldbridge.service;

import de.mhus.nimbus.shared.dto.websocket.WebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.worldbridge.model.WebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorldBridgeService {

    private final List<WebSocketCommand> commands;
    private Map<String, WebSocketCommand> commandMap;

    @PostConstruct
    public void initializeCommands() {
        // Store commands in a map for quick lookup by command name
        commandMap = commands.stream()
                .collect(Collectors.toMap(
                    cmd -> cmd.info().getCommand(),
                    Function.identity()
                ));

        log.info("Initialized {} WebSocket commands: {}",
                commandMap.size(),
                commandMap.keySet());
    }

    public WebSocketResponse processCommand(String sessionId, WebSocketSession sessionInfo,
                                          de.mhus.nimbus.shared.dto.websocket.WebSocketCommand command) {
        try {
            ExecuteResponse executeResponse = executeCommand(sessionId, sessionInfo, command);

            if (executeResponse.isSuccess()) {
                return executeResponse.getResponse();
            } else {
                return WebSocketResponse.builder()
                        .service(command.getService())
                        .command(command.getCommand())
                        .requestId(command.getRequestId())
                        .status("error")
                        .errorCode(executeResponse.getErrorCode())
                        .message(executeResponse.getMessage())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing command: {}", command.getCommand(), e);
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("INTERNAL_ERROR")
                    .message("Internal server error")
                    .build();
        }
    }

    public ExecuteResponse executeCommand(String sessionId, WebSocketSession sessionInfo,
                                        de.mhus.nimbus.shared.dto.websocket.WebSocketCommand command) {
        WebSocketCommand commandHandler = commandMap.get(command.getCommand());

        if (commandHandler == null) {
            return ExecuteResponse.error("UNKNOWN_COMMAND", "Unknown command: " + command.getCommand());
        }

        ExecuteRequest request = new ExecuteRequest(sessionId, sessionInfo, command);
        return commandHandler.execute(request);
    }

    public List<WebSocketCommandInfo> getAvailableCommands() {
        return commands.stream()
                .map(WebSocketCommand::info)
                .collect(Collectors.toList());
    }
}
