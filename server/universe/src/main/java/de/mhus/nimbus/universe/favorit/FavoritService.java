package de.mhus.nimbus.universe.favorit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class FavoritService {

    private final FavoritRepository repository;

    public FavoritService(FavoritRepository repository) {
        this.repository = repository;
    }

    public Favorit create(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favorit) {
        validateIds(userId, quadrantId);
        Favorit f = new Favorit(userId, quadrantId, solarSystemId, worldId, entryPointId, title, favorit);
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    public Favorit createOrUpdateAccess(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favoritFlag) {
        validateIds(userId, quadrantId);
        Optional<Favorit> existing = repository.findByUserIdAndQuadrantIdAndSolarSystemIdAndWorldIdAndEntryPointId(userId, quadrantId, solarSystemId, worldId, entryPointId);
        if (existing.isPresent()) {
            Favorit f = existing.get();
            if (title != null && !title.isBlank()) f.setTitle(title);
            f.setFavorit(favoritFlag || f.isFavorit()); // once true stays true unless explicitly toggled off elsewhere
            f.setLastAccessAt(Instant.now());
            return repository.save(f);
        }
        return create(userId, quadrantId, solarSystemId, worldId, entryPointId, title, favoritFlag);
    }

    public Optional<Favorit> getById(String id) {
        return repository.findById(id).map(this::touchAccess);
    }

    public Optional<Favorit> getByComposite(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId) {
        return repository.findByUserIdAndQuadrantIdAndSolarSystemIdAndWorldIdAndEntryPointId(userId, quadrantId, solarSystemId, worldId, entryPointId)
                .map(this::touchAccess);
    }

    public List<Favorit> listAllByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public List<Favorit> listFavorites(String userId) {
        return repository.findByUserId(userId).stream().filter(Favorit::isFavorit).toList();
    }

    public Favorit toggleFavorite(String id, boolean newState) {
        Favorit f = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Favorit not found: " + id));
        f.setFavorit(newState);
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public Favorit update(String id, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favoritFlag) {
        Favorit f = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Favorit not found: " + id));
        if (quadrantId != null && !quadrantId.isBlank()) f.setQuadrantId(quadrantId);
        if (solarSystemId != null) f.setSolarSystemId(solarSystemId);
        if (worldId != null) f.setWorldId(worldId);
        if (entryPointId != null) f.setEntryPointId(entryPointId);
        if (title != null && !title.isBlank()) f.setTitle(title);
        f.setFavorit(favoritFlag);
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    private Favorit touchAccess(Favorit f) {
        f.setLastAccessAt(Instant.now());
        return repository.save(f);
    }

    private void validateIds(String userId, String quadrantId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId blank");
        if (quadrantId == null || quadrantId.isBlank()) throw new IllegalArgumentException("quadrantId blank");
    }
}
