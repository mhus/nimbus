package de.mhus.nimbus.world.player.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.mhus.nimbus.generated.network.messages.ChunkDataTransferObject;
import de.mhus.nimbus.world.player.service.EditModeService;
import de.mhus.nimbus.world.player.service.ExecutionService;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.world.WChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Central service for sending chunks to clients.
 * Handles chunk loading, overlay application, and network transmission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkSenderService {

    private final WChunkService chunkService;
    private final EditModeService editModeService;
    private final ExecutionService executionService;
    private final ObjectMapper objectMapper;

    /**
     * Send chunks to a client session asynchronously.
     *
     * @param session Player session
     * @param chunks  List of chunk coordinates
     * @return CompletableFuture that completes when chunks are sent
     */
    public CompletableFuture<Void> sendChunksAsync(PlayerSession session, List<ChunkCoord> chunks) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        executionService.execute(() -> {
            try {
                sendChunks(session, chunks);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Send chunks to a client session synchronously.
     *
     * @param session Player session
     * @param chunks  List of chunk coordinates
     */
    public void sendChunks(PlayerSession session, List<ChunkCoord> chunks) {
        try {
            ArrayNode responseChunks = objectMapper.createArrayNode();

            for (ChunkCoord coord : chunks) {
                String chunkKey = coord.cx() + ":" + coord.cz();
                chunkService.loadChunkData(session.getWorldId(), chunkKey, true)
                        .ifPresentOrElse(
                                chunkData -> {
                                    // Apply overlays if session is in edit mode
                                    if (session.isEditMode()) {
                                        editModeService.applyOverlays(session, chunkData);
                                    }

                                    // Convert to transfer object for network transmission (includes items)
                                    ChunkDataTransferObject dto = chunkService.toTransferObject(
                                            session.getWorldId(), session.getWorldId(), chunkData);
                                    responseChunks.add(objectMapper.valueToTree(dto));
                                    log.trace("Loaded chunk: cx={}, cz={}, worldId={}, editMode={}, blocks={}",
                                            coord.cx(), coord.cz(), session.getWorldId(),
                                            session.isEditMode(), chunkData.getBlocks() != null ? chunkData.getBlocks().size() : 0);
                                },
                                () -> {
                                    log.debug("Chunk not found: cx={}, cz={}, worldId={}",
                                            coord.cx(), coord.cz(), session.getWorldId());
                                }
                        );
            }

            // Send chunk update if any chunks loaded
            if (responseChunks.size() > 0) {
                NetworkMessage response = NetworkMessage.builder()
                        .t("c.u")
                        .d(responseChunks)
                        .build();

                String json = objectMapper.writeValueAsString(response);
                session.getWebSocketSession().sendMessage(new TextMessage(json));

                log.debug("Sent {} chunks to session={}", responseChunks.size(),
                        session.getWebSocketSession().getId());
            }
        } catch (Exception e) {
            log.error("Error sending chunks to session={}", session.getWebSocketSession().getId(), e);
            throw new RuntimeException("Failed to send chunks", e);
        }
    }

    /**
     * Chunk coordinate record.
     */
    public record ChunkCoord(int cx, int cz) {}
}
