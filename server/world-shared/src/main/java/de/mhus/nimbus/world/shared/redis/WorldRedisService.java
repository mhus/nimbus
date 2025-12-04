package de.mhus.nimbus.world.shared.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorldRedisService {

    private final StringRedisTemplate redis;

    public void putValue(String worldId, String key, String value, Duration ttl) {
        String namespaced = ns(worldId, key);
        redis.opsForValue().set(namespaced, value, ttl);
    }

    public Optional<String> getValue(String worldId, String key) {
        String namespaced = ns(worldId, key);
        return Optional.ofNullable(redis.opsForValue().get(namespaced));
    }

    public boolean deleteValue(String worldId, String key) {
        String namespaced = ns(worldId, key);
        Boolean res = redis.delete(namespaced);
        return Boolean.TRUE.equals(res);
    }

    /**
     * Store block in overlay hash.
     * Key: world:{worldId}:overlay:{sessionId}:{cx}:{cz}
     * Field: position key (e.g., "15:64:23")
     * Value: Block JSON
     *
     * @param worldId World identifier
     * @param sessionId Session identifier
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @param positionKey Position key ("x:y:z" format)
     * @param blockJson Block data as JSON string
     * @param ttl Time to live for the overlay key
     */
    public void putOverlayBlock(String worldId, String sessionId, int cx, int cz,
                                String positionKey, String blockJson, Duration ttl) {
        String key = overlayKey(worldId, sessionId, cx, cz);
        redis.opsForHash().put(key, positionKey, blockJson);
        redis.expire(key, ttl);
    }

    /**
     * Get all overlay blocks for a chunk.
     * Returns map: position key -> block JSON
     *
     * @param worldId World identifier
     * @param sessionId Session identifier
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return Map of position keys to block JSON strings
     */
    public Map<Object, Object> getOverlayBlocks(String worldId, String sessionId, int cx, int cz) {
        String key = overlayKey(worldId, sessionId, cx, cz);
        return redis.opsForHash().entries(key);
    }

    /**
     * Delete specific overlay block.
     *
     * @param worldId World identifier
     * @param sessionId Session identifier
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @param positionKey Position key to delete
     * @return true if block was deleted
     */
    public boolean deleteOverlayBlock(String worldId, String sessionId, int cx, int cz, String positionKey) {
        String key = overlayKey(worldId, sessionId, cx, cz);
        Long deleted = redis.opsForHash().delete(key, positionKey);
        return deleted != null && deleted > 0;
    }

    /**
     * Delete all overlays for a session (wildcard delete).
     * Pattern: world:{worldId}:overlay:{sessionId}:*
     *
     * @param worldId World identifier
     * @param sessionId Session identifier
     * @return Number of keys deleted
     */
    public long deleteAllOverlays(String worldId, String sessionId) {
        String pattern = ns(worldId, "overlay:" + sessionId + ":*");
        Set<String> keys = redis.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long deleted = redis.delete(keys);
        return deleted != null ? deleted : 0;
    }

    /**
     * Generate overlay key for a specific chunk.
     *
     * @param worldId World identifier
     * @param sessionId Session identifier
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return Redis key for overlay hash
     */
    private String overlayKey(String worldId, String sessionId, int cx, int cz) {
        return ns(worldId, String.format("overlay:%s:%d:%d", sessionId, cx, cz));
    }

    private String ns(String worldId, String key) {
        // Use ':' delimiter to match test expectations and redis key convention
        return "world:" + worldId + ":" + key;
    }
}
