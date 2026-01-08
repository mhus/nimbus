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
        Optional<WPlayerSession> existingOpt = repository.findByWorldIdAndPlayerId(worldId, playerId);

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
        return repository.findByWorldIdAndPlayerId(worldId, playerId);
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
}
