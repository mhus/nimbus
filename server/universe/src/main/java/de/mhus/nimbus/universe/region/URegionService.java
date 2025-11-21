package de.mhus.nimbus.universe.region;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class URegionService {

    private final URegionRepository repository;

    public URegionService(URegionRepository repository) {
        this.repository = repository;
    }

    public URegion create(String name, String apiUrl, String publicSignKey, String maintainersCsv) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
        URegion q = new URegion(name, apiUrl, publicSignKey);
        q.setMaintainers(maintainersCsv); // CSV normalisieren
        return repository.save(q);
    }

    public URegion create(String name, String apiUrl, String publicSignKey) {
        return create(name, apiUrl, publicSignKey, null);
    }

    public Optional<URegion> getById(String id) { return repository.findById(id); }
    public Optional<URegion> getByName(String name) { return repository.findByName(name); }
    public List<URegion> listAll() { return repository.findAll(); }

    public URegion update(String id, String name, String apiUrl, String publicSignKey, String maintainersCsv) {
        URegion existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        if (name != null && !name.isBlank() && !name.equals(existing.getName())) {
            if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
            existing.setName(name);
        }
        if (apiUrl != null && !apiUrl.isBlank() && !apiUrl.equals(existing.getApiUrl())) {
            if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
            existing.setApiUrl(apiUrl);
        }
        if (publicSignKey != null) existing.setPublicSignKeyId(publicSignKey);
        if (maintainersCsv != null) existing.setMaintainers(maintainersCsv);
        return repository.save(existing);
    }

    public URegion addMaintainer(String id, String userId) {
        URegion existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        existing.addMaintainer(userId);
        return repository.save(existing);
    }

    public URegion removeMaintainer(String id, String userId) {
        URegion existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        existing.removeMaintainer(userId);
        return repository.save(existing);
    }

    public void delete(String id) { repository.deleteById(id); }
}
