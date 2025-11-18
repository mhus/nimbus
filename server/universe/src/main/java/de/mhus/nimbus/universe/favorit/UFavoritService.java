package de.mhus.nimbus.universe.favorit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UFavoritService {

    private final UFavoritRepository repository;

    public UFavoritService(UFavoritRepository repository) {
        this.repository = repository;
    }

    public UFavorit create(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favorit) {
        validateIds(userId, quadrantId);
        UFavorit f = new UFavorit(userId, quadrantId, solarSystemId, worldId, entryPointId, title, favorit);
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    public UFavorit createOrUpdateAccess(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favoritFlag) {
        validateIds(userId, quadrantId);
        Optional<UFavorit> existing = repository.findByUserIdAndQuadrantIdAndSolarSystemIdAndWorldIdAndEntryPointId(userId, quadrantId, solarSystemId, worldId, entryPointId);
        if (existing.isPresent()) {
            UFavorit f = existing.get();
            if (title != null && !title.isBlank()) f.setTitle(title);
            f.setFavorit(favoritFlag || f.isFavorit()); // once true stays true unless explicitly toggled off elsewhere
            f.setLastAccessAt(Instant.now());
            return repository.save(f);
        }
        return create(userId, quadrantId, solarSystemId, worldId, entryPointId, title, favoritFlag);
    }

    public Optional<UFavorit> getById(String id) {
        return repository.findById(id).map(this::touchAccess);
    }

    public Optional<UFavorit> getByComposite(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId) {
        return repository.findByUserIdAndQuadrantIdAndSolarSystemIdAndWorldIdAndEntryPointId(userId, quadrantId, solarSystemId, worldId, entryPointId)
                .map(this::touchAccess);
    }

    public List<UFavorit> listAllByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public List<UFavorit> listFavorites(String userId) {
        return repository.findByUserId(userId).stream().filter(UFavorit::isFavorit).toList();
    }

    public UFavorit toggleFavorite(String id, boolean newState) {
        UFavorit f = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Favorit not found: " + id));
        f.setFavorit(newState);
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public UFavorit update(String id, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favoritFlag) {
        UFavorit f = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Favorit not found: " + id));
        if (quadrantId != null && !quadrantId.isBlank()) f.setQuadrantId(quadrantId);
        if (solarSystemId != null) f.setSolarSystemId(solarSystemId);
        if (worldId != null) f.setWorldId(worldId);
        if (entryPointId != null) f.setEntryPointId(entryPointId);
        if (title != null && !title.isBlank()) f.setTitle(title);
        f.setFavorit(favoritFlag);
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    private UFavorit touchAccess(UFavorit f) {
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    private void validateIds(String userId, String quadrantId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId blank");
        if (quadrantId == null || quadrantId.isBlank()) throw new IllegalArgumentException("quadrantId blank");
    }
}
