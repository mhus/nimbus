package de.mhus.nimbus.world.bridge.service;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.command.*;
import de.mhus.nimbus.world.bridge.model.WebSocketSession;
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

    public WorldWebSocketResponse processCommand(String sessionId, WebSocketSession sessionInfo,
                                                 WorldWebSocketCommand command) {
        try {
            ExecuteResponse executeResponse = executeCommand(sessionId, sessionInfo, command);

            if (executeResponse.isSuccess()) {
                return executeResponse.getResponse();
            } else {
                return WorldWebSocketResponse.builder()
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
            return WorldWebSocketResponse.builder()
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
                                        WorldWebSocketCommand command) {
        WebSocketCommand commandHandler = commandMap.get(command.getCommand());

        if (commandHandler == null) {
            return ExecuteResponse.error("UNKNOWN_COMMAND", "Unknown command: " + command.getCommand());
        }

        WebSocketCommandInfo commandInfo = commandHandler.info();

        // Check authentication requirement
        if (commandInfo.isAuthenticationRequired() && !sessionInfo.isLoggedIn()) {
            return ExecuteResponse.error("NOT_AUTHENTICATED", "User not authenticated");
        }

        // Check world requirement
        if (commandInfo.isWorldRequired() && !sessionInfo.hasWorld()) {
            return ExecuteResponse.error("NO_WORLD_SELECTED", "No world selected");
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
