package de.mhus.nimbus.quadrant.registry;

import java.util.List;
import java.util.Optional;

import de.mhus.nimbus.universe.quadrant.UQuadrant;
import de.mhus.nimbus.universe.quadrant.UQuadrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UQuadrantService {

    private final UQuadrantRepository repository;

    public UQuadrantService(UQuadrantRepository repository) {
        this.repository = repository;
    }

    public UQuadrant create(String name, String apiUrl, String publicSignKey, String maintainers) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("UQuadrant name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("UQuadrant apiUrl exists: " + apiUrl);
        UQuadrant q = new UQuadrant(name, apiUrl, publicSignKey);
        q.setMaintainers(maintainers);
        return repository.save(q);
    }

    // Bestehende create-Methode bleibt für Abwärtskompatibilität
    public UQuadrant create(String name, String apiUrl, String publicSignKey) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("UQuadrant name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("UQuadrant apiUrl exists: " + apiUrl);
        UQuadrant q = new UQuadrant(name, apiUrl, publicSignKey);
        return repository.save(q);
    }

    public Optional<UQuadrant> getById(String id) {
        return repository.findById(id);
    }

    public Optional<UQuadrant> getByName(String name) {
        return repository.findByName(name);
    }

    public List<UQuadrant> listAll() {
        return repository.findAll();
    }

    public UQuadrant update(String id, String name, String apiUrl, String publicSignKey, String maintainers) {
        UQuadrant existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UQuadrant not found: " + id));
        if (name != null && !name.isBlank() && !name.equals(existing.getName())) {
            if (repository.existsByName(name)) throw new IllegalArgumentException("UQuadrant name exists: " + name);
            existing.setName(name);
        }
        if (apiUrl != null && !apiUrl.isBlank() && !apiUrl.equals(existing.getApiUrl())) {
            if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("UQuadrant apiUrl exists: " + apiUrl);
            existing.setApiUrl(apiUrl);
        }
        if (publicSignKey != null) existing.setPublicSignKey(publicSignKey);
        if (maintainers != null) existing.setMaintainers(maintainers);
        return repository.save(existing);
    }

    public UQuadrant addMaintainer(String id, String userId) {
        UQuadrant existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("UQuadrant not found: " + id));
        existing.addMaintainer(userId);
        return repository.save(existing);
    }

    public UQuadrant removeMaintainer(String id, String userId) {
        UQuadrant existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("UQuadrant not found: " + id));
        existing.removeMaintainer(userId);
        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
