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
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

                // First find WChunk entity
                var chunkOpt = chunkService.find(session.getWorldId(), chunkKey);
                if (chunkOpt.isEmpty()) {
                    // Generate default chunk if not found
                    var chunkData = chunkService.loadChunkData(session.getWorldId(), chunkKey, true);
                    if (chunkData.isEmpty()) {
                        log.debug("Chunk not found and could not generate: cx={}, cz={}", coord.cx(), coord.cz());
                        continue;
                    }
                    // Save generated chunk
                    var saved = chunkService.saveChunk(session.getWorldId(), chunkKey, chunkData.get());
                    chunkOpt = java.util.Optional.of(saved);
                }

                var chunk = chunkOpt.get();

                // Handle edit mode overlays (requires loading ChunkData)
                if (session.isEditMode()) {
                    var chunkData = chunkService.loadChunkData(session.getWorldId(), chunkKey, false);
                    if (chunkData.isPresent()) {
                        editModeService.applyOverlays(session, chunkData.get());
                        // Save modified chunk and update entity
                        chunk = chunkService.saveChunk(session.getWorldId(), chunkKey, chunkData.get());
                    }
                }

                // Convert to transfer object (uses compressed storage if available)
                ChunkDataTransferObject dto = chunkService.toTransferObject(session.getWorldId(), chunk);
                if (dto == null) {
                    log.warn("Failed to convert chunk to transfer object: chunkKey={}", chunkKey);
                    continue;
                }

                // Send as binary frame if compressed, otherwise add to JSON array
                if (dto.getC() != null && dto.getC().length > 0) {
                    try {
                        sendCompressedChunkBinary(session, dto);
                        log.info("✓ Sent binary compressed chunk: cx={}, cz={}, compressed={} bytes",
                                coord.cx(), coord.cz(), dto.getC().length);
                    } catch (Exception e) {
                        log.error("Failed to send binary chunk, falling back to text: cx={}, cz={}",
                                coord.cx(), coord.cz(), e);
                        responseChunks.add(objectMapper.valueToTree(dto));
                    }
                } else {
                    responseChunks.add(objectMapper.valueToTree(dto));
                    log.trace("Sent uncompressed chunk: cx={}, cz={}, blocks={}",
                            coord.cx(), coord.cz(), dto.getB() != null ? dto.getB().size() : 0);
                }
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
     * Send compressed chunk as binary WebSocket frame.
     * Format: [4 bytes header length][header JSON][GZIP compressed data]
     */
    private void sendCompressedChunkBinary(PlayerSession session, ChunkDataTransferObject dto) throws Exception {
        // 1. Build header with metadata (small data, stays JSON)
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("cx", dto.getCx());
        header.put("cz", dto.getCz());
        if (dto.getI() != null && !dto.getI().isEmpty()) {
            header.put("i", dto.getI());
        }

        String headerJson = objectMapper.writeValueAsString(header);
        byte[] headerBytes = headerJson.getBytes(StandardCharsets.UTF_8);

        // 2. Build binary frame: [4 bytes length][header][compressed data]
        ByteBuffer buffer = ByteBuffer.allocate(4 + headerBytes.length + dto.getC().length);
        buffer.putInt(headerBytes.length);  // Header length as int32 (big-endian)
        buffer.put(headerBytes);             // Header JSON
        buffer.put(dto.getC());              // GZIP compressed data

        // 3. Send as binary WebSocket frame
        session.getWebSocketSession().sendMessage(new BinaryMessage(buffer.array()));

        log.debug("Sent binary chunk: cx={}, cz={}, header={} bytes, compressed={} bytes, total={} bytes",
                dto.getCx(), dto.getCz(), headerBytes.length, dto.getC().length, buffer.position());
    }

    /**
     * Chunk coordinate record.
     */
    public record ChunkCoord(int cx, int cz) {}
}
