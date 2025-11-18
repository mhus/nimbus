package de.mhus.nimbus.quadrant.registry;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class QuadrantService {

    private final QuadrantRepository repository;

    public QuadrantService(QuadrantRepository repository) {
        this.repository = repository;
    }

    public Quadrant create(String name, String apiUrl, String publicSignKey, String maintainers) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Quadrant name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Quadrant apiUrl exists: " + apiUrl);
        Quadrant q = new Quadrant(name, apiUrl, publicSignKey);
        q.setMaintainers(maintainers);
        return repository.save(q);
    }

    // Bestehende create-Methode bleibt für Abwärtskompatibilität
    public Quadrant create(String name, String apiUrl, String publicSignKey) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Quadrant name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Quadrant apiUrl exists: " + apiUrl);
        Quadrant q = new Quadrant(name, apiUrl, publicSignKey);
        return repository.save(q);
    }

    public Optional<Quadrant> getById(String id) {
        return repository.findById(id);
    }

    public Optional<Quadrant> getByName(String name) {
        return repository.findByName(name);
    }

    public List<Quadrant> listAll() {
        return repository.findAll();
    }

    public Quadrant update(String id, String name, String apiUrl, String publicSignKey, String maintainers) {
        Quadrant existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Quadrant not found: " + id));
        if (name != null && !name.isBlank() && !name.equals(existing.getName())) {
            if (repository.existsByName(name)) throw new IllegalArgumentException("Quadrant name exists: " + name);
            existing.setName(name);
        }
        if (apiUrl != null && !apiUrl.isBlank() && !apiUrl.equals(existing.getApiUrl())) {
            if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Quadrant apiUrl exists: " + apiUrl);
            existing.setApiUrl(apiUrl);
        }
        if (publicSignKey != null) existing.setPublicSignKey(publicSignKey);
        if (maintainers != null) existing.setMaintainers(maintainers);
        return repository.save(existing);
    }

    public Quadrant addMaintainer(String id, String userId) {
        Quadrant existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Quadrant not found: " + id));
        existing.addMaintainer(userId);
        return repository.save(existing);
    }

    public Quadrant removeMaintainer(String id, String userId) {
        Quadrant existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Quadrant not found: " + id));
        existing.removeMaintainer(userId);
        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
