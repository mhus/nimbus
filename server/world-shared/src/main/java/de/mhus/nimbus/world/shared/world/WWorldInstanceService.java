package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.shared.types.WorldId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic service for WWorldInstance.
 * Manages world instances which are independent copies of worlds.
 * Supports listeners for instance lifecycle events.
 */
@Service
@Slf4j
public class WWorldInstanceService {

    private final WWorldInstanceRepository repository;
    private final WWorldService worldService;
    private final List<WWorldInstanceListener> listeners;

    /**
     * Constructor with lazy listener initialization to avoid circular dependencies.
     *
     * @param repository The repository
     * @param worldService The world service
     * @param listeners List of listeners (lazy initialized)
     */
    public WWorldInstanceService(
            WWorldInstanceRepository repository,
            WWorldService worldService,
            @Lazy List<WWorldInstanceListener> listeners) {
        this.repository = repository;
        this.worldService = worldService;
        this.listeners = listeners;
    }

    /**
     * Find all world instances.
     *
     * @return List of all instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findAll() {
        return repository.findAll();
    }

    /**
     * Find a world instance by instanceId.
     *
     * @param instanceId The instanceId
     * @return Optional containing the instance if found
     */
    @Transactional(readOnly = true)
    public Optional<WWorldInstance> findByInstanceId(String instanceId) {
        return repository.findByInstanceId(instanceId);
    }

    /**
     * Find all instances based on a specific world.
     *
     * @param worldId The worldId
     * @return List of instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    /**
     * Find all instances based on a specific world (WorldId object).
     *
     * @param worldId The WorldId object
     * @return List of instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    /**
     * Find all instances created by a specific player.
     *
     * @param creator The playerId of the creator
     * @return List of instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findByCreator(String creator) {
        return repository.findByCreator(creator);
    }

    /**
     * Find all instances where a specific player is allowed.
     *
     * @param playerId The playerId
     * @return List of instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findByPlayer(String playerId) {
        return repository.findByPlayersContaining(playerId);
    }

    /**
     * Find all enabled instances based on a specific world.
     *
     * @param worldId The worldId
     * @return List of enabled instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findEnabledByWorldId(String worldId) {
        return repository.findByWorldIdAndEnabled(worldId, true);
    }

    /**
     * Find all enabled instances created by a specific player.
     *
     * @param creator The playerId of the creator
     * @return List of enabled instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findEnabledByCreator(String creator) {
        return repository.findByCreatorAndEnabled(creator, true);
    }

    /**
     * Check if an instance exists by instanceId.
     *
     * @param instanceId The instanceId
     * @return true if exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByInstanceId(String instanceId) {
        return repository.existsByInstanceId(instanceId);
    }

    /**
     * Create a new world instance.
     * Validates that the base world exists.
     *
     * @param instanceId The unique instanceId
     * @param worldId The worldId this instance is based on
     * @param title The title
     * @param description The description
     * @param creator The playerId of the creator
     * @param players List of playerIds allowed to access
     * @return The created instance
     * @throws IllegalStateException if instance already exists
     * @throws IllegalArgumentException if world does not exist
     */
    @Transactional
    public WWorldInstance create(String instanceId, String worldId, String title, String description,
                                   String creator, List<String> players) {
        if (repository.existsByInstanceId(instanceId)) {
            throw new IllegalStateException("Instance already exists: " + instanceId);
        }

        // Validate that world exists
        if (!worldService.getByWorldId(worldId).isPresent()) {
            throw new IllegalArgumentException("World does not exist: " + worldId);
        }

        WWorldInstance instance = WWorldInstance.builder()
                .instanceId(instanceId)
                .worldId(worldId)
                .title(title)
                .description(description)
                .creator(creator)
                .players(players != null ? players : List.of())
                .enabled(true)
                .build();
        instance.touchCreate();
        repository.save(instance);

        log.debug("World instance created: instanceId={}, worldId={}, creator={}",
                instanceId, worldId, creator);

        // Notify listeners
        notifyListenersCreated(instance);

        return instance;
    }

    /**
     * Update an existing world instance.
     *
     * @param instanceId The instanceId
     * @param updater Consumer to update the instance
     * @return Optional containing the updated instance
     */
    @Transactional
    public Optional<WWorldInstance> update(String instanceId, java.util.function.Consumer<WWorldInstance> updater) {
        return repository.findByInstanceId(instanceId).map(existing -> {
            updater.accept(existing);
            existing.touchUpdate();
            repository.save(existing);
            log.debug("World instance updated: instanceId={}", instanceId);
            return existing;
        });
    }

    /**
     * Save a world instance (update timestamp and persist).
     *
     * @param instance The instance to save
     * @return The saved instance
     */
    @Transactional
    public WWorldInstance save(WWorldInstance instance) {
        instance.touchUpdate();
        WWorldInstance saved = repository.save(instance);
        log.debug("World instance saved: instanceId={}", instance.getInstanceId());
        return saved;
    }

    /**
     * Add a player to an instance.
     *
     * @param instanceId The instanceId
     * @param playerId The playerId to add
     * @return true if player was added, false if already present or instance not found
     */
    @Transactional
    public boolean addPlayer(String instanceId, String playerId) {
        return repository.findByInstanceId(instanceId).map(instance -> {
            boolean added = instance.addPlayer(playerId);
            if (added) {
                save(instance);
                log.debug("Player added to instance: instanceId={}, playerId={}", instanceId, playerId);
            }
            return added;
        }).orElse(false);
    }

    /**
     * Remove a player from an instance.
     *
     * @param instanceId The instanceId
     * @param playerId The playerId to remove
     * @return true if player was removed, false if not present or instance not found
     */
    @Transactional
    public boolean removePlayer(String instanceId, String playerId) {
        return repository.findByInstanceId(instanceId).map(instance -> {
            boolean removed = instance.removePlayer(playerId);
            if (removed) {
                save(instance);
                log.debug("Player removed from instance: instanceId={}, playerId={}", instanceId, playerId);
            }
            return removed;
        }).orElse(false);
    }

    /**
     * Check if a player has access to an instance.
     *
     * @param instanceId The instanceId
     * @param playerId The playerId
     * @return true if player has access, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPlayerAccess(String instanceId, String playerId) {
        return repository.findByInstanceId(instanceId)
                .map(instance -> instance.isPlayerAllowed(playerId))
                .orElse(false);
    }

    /**
     * Delete a world instance by instanceId.
     *
     * @param instanceId The instanceId
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(String instanceId) {
        Optional<WWorldInstance> instanceOpt = repository.findByInstanceId(instanceId);
        if (instanceOpt.isEmpty()) {
            return false;
        }

        WWorldInstance instance = instanceOpt.get();

        // Delete from repository
        repository.deleteByInstanceId(instanceId);
        log.debug("World instance deleted: instanceId={}", instanceId);

        // Notify listeners
        notifyListenersDeleted(instance);

        return true;
    }

    /**
     * Count instances by worldId.
     *
     * @param worldId The worldId
     * @return Number of instances
     */
    @Transactional(readOnly = true)
    public long countByWorldId(String worldId) {
        return repository.countByWorldId(worldId);
    }

    /**
     * Count instances by creator.
     *
     * @param creator The playerId of the creator
     * @return Number of instances
     */
    @Transactional(readOnly = true)
    public long countByCreator(String creator) {
        return repository.countByCreator(creator);
    }

    /**
     * Find all instances accessible by a player.
     * Includes instances where player is creator or in players list.
     *
     * @param playerId The playerId
     * @return List of accessible instances
     */
    @Transactional(readOnly = true)
    public List<WWorldInstance> findAccessibleByPlayer(String playerId) {
        // Get instances where player is creator
        List<WWorldInstance> createdInstances = repository.findByCreator(playerId);

        // Get instances where player is in players list
        List<WWorldInstance> playerInstances = repository.findByPlayersContaining(playerId);

        // Combine both lists (use stream to avoid duplicates)
        return java.util.stream.Stream.concat(
                createdInstances.stream(),
                playerInstances.stream()
        ).distinct().toList();
    }

    /**
     * Notify all registered listeners that an instance was created.
     *
     * @param instance The created instance
     */
    private void notifyListenersCreated(WWorldInstance instance) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        WorldInstanceEvent event = WorldInstanceEvent.created(instance);
        for (WWorldInstanceListener listener : listeners) {
            try {
                listener.worldInstanceCreated(event);
            } catch (Exception e) {
                log.error("Error notifying listener {} about instance creation: {}",
                        listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        log.debug("Notified {} listeners about instance creation: instanceId={}",
                listeners.size(), instance.getInstanceId());
    }

    /**
     * Notify all registered listeners that an instance was deleted.
     *
     * @param instance The deleted instance
     */
    private void notifyListenersDeleted(WWorldInstance instance) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        WorldInstanceEvent event = WorldInstanceEvent.deleted(instance);
        for (WWorldInstanceListener listener : listeners) {
            try {
                listener.worldInstanceDeleted(event);
            } catch (Exception e) {
                log.error("Error notifying listener {} about instance deletion: {}",
                        listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        log.debug("Notified {} listeners about instance deletion: instanceId={}",
                listeners.size(), instance.getInstanceId());
    }
}
