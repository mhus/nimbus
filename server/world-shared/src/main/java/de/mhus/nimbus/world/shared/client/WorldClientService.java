package de.mhus.nimbus.world.shared.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Outbound REST client for inter-server command communication.
 * Provides async command execution with CompletableFuture and timeout support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorldClientService {

    @Qualifier("worldRestTemplate")
    private final RestTemplate restTemplate;
    private final WorldClientProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Command request DTO.
     */
    public record CommandRequest(
            String cmd,
            List<String> args,
            String worldId,
            String sessionId,
            String userId,
            String displayName,
            String originServer,
            Map<String, Object> metadata
    ) {}

    /**
     * Command response DTO.
     */
    public record CommandResponse(
            int rc,
            String message,
            List<String> streamMessages
    ) {}

    /**
     * Send command to world-life server.
     *
     * @param worldId World identifier
     * @param commandName Command name
     * @param args Command arguments
     * @param context Optional context (for session data)
     * @return CompletableFuture with CommandResponse
     */
    public CompletableFuture<CommandResponse> sendLifeCommand(
            String worldId,
            String commandName,
            List<String> args,
            CommandContext context) {

        String baseUrl = properties.getLifeBaseUrl();
        return sendCommand(baseUrl, worldId, commandName, args, context, "world-life");
    }

    /**
     * Send command to world-player server.
     *
     * @param worldId World identifier
     * @param sessionId Session identifier (optional)
     * @param origin Origin server IP + : + port
     * @param commandName Command name
     * @param args Command arguments
     * @param context Optional context
     * @return CompletableFuture with CommandResponse
     */
    public CompletableFuture<CommandResponse> sendPlayerCommand(
            String worldId,
            String sessionId,
            String origin,
            String commandName,
            List<String> args,
            CommandContext context) {

        String baseUrl = properties.getPlayerBaseUrl();

        // Update context with session if provided
        if (context == null) {
            context = CommandContext.builder()
                    .worldId(worldId)
                    .sessionId(sessionId)
                    .build();
        } else if (sessionId != null) {
            context.setSessionId(sessionId);
        }

        return sendCommand(baseUrl, worldId, commandName, args, context, "world-player");
    }

    /**
     * Send command to world-control server.
     *
     * @param worldId World identifier
     * @param commandName Command name
     * @param args Command arguments
     * @param context Optional context
     * @return CompletableFuture with CommandResponse
     */
    public CompletableFuture<CommandResponse> sendControlCommand(
            String worldId,
            String commandName,
            List<String> args,
            CommandContext context) {

        String baseUrl = properties.getControlBaseUrl();
        return sendCommand(baseUrl, worldId, commandName, args, context, "world-control");
    }

    /**
     * Generic command sender.
     */
    private CompletableFuture<CommandResponse> sendCommand(
            String baseUrl,
            String worldId,
            String commandName,
            List<String> args,
            CommandContext context,
            String targetServer) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build request
                CommandRequest request = new CommandRequest(
                        commandName,
                        args != null ? args : List.of(),
                        worldId,
                        context != null ? context.getSessionId() : null,
                        context != null ? context.getUserId() : null,
                        context != null ? context.getDisplayName() : null,
                        context != null ? context.getOriginServer() : "unknown",
                        context != null ? context.getMetadata() : null
                );

                // Build URL
                String url = baseUrl + "/world/world/command/" + encode(commandName);

                log.debug("Sending command to {}: url={}, cmd={}, worldId={}",
                        targetServer, url, commandName, worldId);

                // Execute REST call with timeout
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        URI.create(url),
                        request,
                        Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();

                    int rc = body.get("rc") instanceof Number n ? n.intValue() : -4;
                    String message = (String) body.get("message");

                    @SuppressWarnings("unchecked")
                    List<String> streamMessages = body.get("streamMessages") instanceof List list
                            ? (List<String>) list
                            : null;

                    log.debug("Command completed: cmd={}, rc={}, target={}",
                            commandName, rc, targetServer);

                    return new CommandResponse(rc, message, streamMessages);
                }

                log.error("Unexpected response status: {}", response.getStatusCode());
                return new CommandResponse(-4, "Server error: " + response.getStatusCode(), null);

            } catch (RestClientException e) {
                log.error("Command failed: cmd={}, target={}, error={}",
                        commandName, targetServer, e.getMessage());
                return new CommandResponse(-4, "Communication error: " + e.getMessage(), null);

            } catch (Exception e) {
                log.error("Unexpected error sending command", e);
                return new CommandResponse(-4, "Internal error: " + e.getMessage(), null);
            }
        })
        .orTimeout(properties.getCommandTimeoutMs(), TimeUnit.MILLISECONDS)
        .exceptionally(throwable -> {
            if (throwable instanceof TimeoutException) {
                log.error("Command timeout: cmd={}, target={}", commandName, targetServer);
                return new CommandResponse(-5, "Command timeout", null);
            }
            log.error("Command execution failed", throwable);
            return new CommandResponse(-4, "Execution error: " + throwable.getMessage(), null);
        });
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
