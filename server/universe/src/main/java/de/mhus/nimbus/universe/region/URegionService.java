package de.mhus.nimbus.universe.region;

import java.util.List;
import java.util.Optional;

import de.mhus.nimbus.shared.security.FormattedKey;
import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.universe.security.RequestUserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class URegionService {

    private final URegionRepository repository;
    private final KeyService keyService;

    public URegion create(String name, String apiUrl, String maintainersCsv, String publicSignKey) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (publicSignKey == null || publicSignKey.isBlank()) throw new IllegalArgumentException("publicSignKey must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
        URegion q = new URegion(name, apiUrl);
        q.setMaintainers(maintainersCsv); // CSV normalisieren
        var formattedKey = FormattedKey.of(publicSignKey).orElseGet(
                () -> FormattedKey.of(KeyId.newOf(KeyIntent.of(name, KeyIntent.REGION_JWT_TOKEN)), publicSignKey).get()
        );
        keyService.storePublicKey(KeyType.REGION, formattedKey);
        return repository.save(q);
    }

    public Optional<URegion> getById(String id) { return repository.findById(id); }
    public Optional<URegion> getByName(String name) { return repository.findByName(name); }
    public List<URegion> listAll() { return repository.findAll(); }

    public URegion update(String id, String name, String apiUrl, String maintainersCsv) {
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
