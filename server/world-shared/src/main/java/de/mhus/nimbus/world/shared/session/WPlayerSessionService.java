package de.mhus.nimbus.world.shared.session;

import de.mhus.nimbus.generated.types.Rotation;
import de.mhus.nimbus.generated.types.Vector3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic service for WPlayerSession.
 * Manages player session state persistence (position, rotation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WPlayerSessionService {

    private final WPlayerSessionRepository repository;

    /**
     * Save or update player session (upsert).
     * This method is idempotent and safe to call repeatedly.
     * Creates a new session if it doesn't exist, or updates an existing session.
     *
     * @param worldId The full worldId (including instance)
     * @param playerId The playerId
     * @param position The player position
     * @param rotation The player rotation
     * @return The saved session
     * @throws IllegalArgumentException if worldId or playerId is null or blank
     */
    @Transactional
    public WPlayerSession saveSession(String worldId, String playerId,
                                       Vector3 position, Rotation rotation) {
        // Validation
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId cannot be null or blank");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId cannot be null or blank");
        }

        // Upsert: find existing or create new
        Optional<WPlayerSession> existingOpt = repository.findFirstByWorldIdAndPlayerIdOrderByUpdatedAtDesc(worldId, playerId);

        WPlayerSession session;
        if (existingOpt.isPresent()) {
            // Update existing
            session = existingOpt.get();
            session.setPosition(position);
            session.setRotation(rotation);
            session.touchUpdate();
            log.debug("Updated player session: worldId={}, playerId={}", worldId, playerId);
        } else {
            // Create new
            session = WPlayerSession.builder()
                    .worldId(worldId)
                    .playerId(playerId)
                    .position(position)
                    .rotation(rotation)
                    .build();
            session.touchCreate();
            log.debug("Created player session: worldId={}, playerId={}", worldId, playerId);
        }

        repository.save(session);
        return session;
    }

    /**
     * Load player session by worldId and playerId.
     *
     * @param worldId The full worldId (including instance)
     * @param playerId The playerId
     * @return Optional containing the session if found
     */
    @Transactional(readOnly = true)
    public Optional<WPlayerSession> loadSession(String worldId, String playerId) {
        return repository.findFirstByWorldIdAndPlayerIdOrderByUpdatedAtDesc(worldId, playerId);
    }

    /**
     * Delete player session by worldId and playerId.
     *
     * @param worldId The full worldId (including instance)
     * @param playerId The playerId
     * @return true if session was deleted, false if not found
     */
    @Transactional
    public boolean deleteSession(String worldId, String playerId) {
        if (repository.existsByWorldIdAndPlayerId(worldId, playerId)) {
            repository.deleteByWorldIdAndPlayerId(worldId, playerId);
            log.debug("Deleted player session: worldId={}, playerId={}", worldId, playerId);
            return true;
        }
        return false;
    }

    /**
     * Find all sessions for a specific world.
     * Useful for admin/debugging purposes.
     *
     * @param worldId The full worldId (including instance)
     * @return List of sessions
     */
    @Transactional(readOnly = true)
    public List<WPlayerSession> findByWorld(String worldId) {
        return repository.findByWorldId(worldId);
    }

    /**
     * Find all sessions for a specific player.
     * Useful for admin/debugging purposes.
     *
     * @param playerId The playerId
     * @return List of sessions
     */
    @Transactional(readOnly = true)
    public List<WPlayerSession> findByPlayer(String playerId) {
        return repository.findByPlayerId(playerId);
    }

    /**
     * Count sessions by worldId.
     *
     * @param worldId The full worldId (including instance)
     * @return Number of sessions
     */
    @Transactional(readOnly = true)
    public long countByWorld(String worldId) {
        return repository.countByWorldId(worldId);
    }

    /**
     * Count sessions by playerId.
     *
     * @param playerId The playerId
     * @return Number of sessions
     */
    @Transactional(readOnly = true)
    public long countByPlayer(String playerId) {
        return repository.countByPlayerId(playerId);
    }

    /**
     * Create new player session for teleportation.
     * This method creates a new session and stores previous world/position/rotation data.
     * Used when player teleports to a new world.
     *
     * @param worldId New worldId (including instance)
     * @param playerId Player ID
     * @param sessionId Session ID reference
     * @param actor Actor type (PLAYER, EDITOR, SUPPORT)
     * @param previousWorldId Previous worldId before teleport
     * @param previousPosition Previous position before teleport
     * @param previousRotation Previous rotation before teleport
     * @return The created session
     * @throws IllegalArgumentException if worldId or playerId is null or blank
     */
    @Transactional
    public WPlayerSession createTeleportSession(String worldId, String playerId,
                                                  String sessionId, String actor,
                                                  String previousWorldId,
                                                  Vector3 previousPosition,
                                                  Rotation previousRotation) {
        // Validation
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId cannot be null or blank");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId cannot be null or blank");
        }

        // Create new session with previous values
        WPlayerSession session = WPlayerSession.builder()
                .worldId(worldId)
                .playerId(playerId)
                .sessionId(sessionId)
                .actor(actor)
                .position(null) // Will be set when player enters world
                .rotation(null) // Will be set when player enters world
                .previousWorldId(previousWorldId)
                .previousPosition(previousPosition)
                .previousRotation(previousRotation)
                .build();
        session.touchCreate();

        repository.save(session);

        log.info("Created teleport player session: worldId={}, playerId={}, previousWorldId={}",
                worldId, playerId, previousWorldId);

        return session;
    }

    /**
     * Merge player status data from old session to new session.
     * Placeholder for future implementation.
     * This will transfer player state (health, mana, stamina, effects, inventory, etc.)
     * when the data model is ready.
     *
     * @param newSession The new target session
     * @param oldSession The old source session
     */
    @Transactional
    public void mergePlayerData(WPlayerSession newSession, WPlayerSession oldSession) {
        if (oldSession == null || newSession == null) {
            return;
        }

        log.debug("mergePlayerData called but not yet implemented: oldWorldId={}, newWorldId={}, playerId={}",
                oldSession.getWorldId(), newSession.getWorldId(), newSession.getPlayerId());

        // TODO: Implement player data merge when fields are defined
        // This will include: health, mana, stamina, effects, inventory, attributes, quest data, etc.
    }
}
