package de.mhus.nimbus.region.registry;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class RRegionService {

    private final RRegionRepository repository;

    public RRegionService(RRegionRepository repository) {
        this.repository = repository;
    }

    public RRegion create(String name, String apiUrl, String publicSignKey, String maintainers) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
        RRegion q = new RRegion(name, apiUrl, publicSignKey);
        q.setMaintainers(maintainers);
        return repository.save(q);
    }

    // Bestehende create-Methode bleibt für Abwärtskompatibilität
    public RRegion create(String name, String apiUrl, String publicSignKey) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
        RRegion q = new RRegion(name, apiUrl, publicSignKey);
        return repository.save(q);
    }

    public Optional<RRegion> getById(String id) {
        return repository.findById(id);
    }

    public Optional<RRegion> getByName(String name) {
        return repository.findByName(name);
    }

    public List<String> listAllIds() {
        return repository.findAllIds();
    }

    public RRegion update(String id, String name, String apiUrl, String publicSignKey, String maintainers) {
        RRegion existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        if (name != null && !name.isBlank() && !name.equals(existing.getName())) {
            if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
            existing.setName(name);
        }
        if (apiUrl != null && !apiUrl.isBlank() && !apiUrl.equals(existing.getApiUrl())) {
            if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
            existing.setApiUrl(apiUrl);
        }
        if (publicSignKey != null) existing.setPublicSignKey(publicSignKey);
        if (maintainers != null) existing.setMaintainers(maintainers);
        return repository.save(existing);
    }

    public RRegion addMaintainer(String id, String userId) {
        RRegion existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        existing.addMaintainer(userId);
        return repository.save(existing);
    }

    public RRegion removeMaintainer(String id, String userId) {
        RRegion existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        existing.removeMaintainer(userId);
        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
