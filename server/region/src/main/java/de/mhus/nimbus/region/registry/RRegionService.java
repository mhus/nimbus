package de.mhus.nimbus.region.registry;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mhus.nimbus.region.world.RUniverseClientService;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.security.KeyIntent;

@Service
@Validated
@Slf4j
public class RRegionService {

    private final RRegionRepository repository;
    private final KeyService keyService; // neu
    private final JwtService jwtService; // neu
    private final RUniverseClientService universeClientService; // neu

    @Value("${universe.base-url:http://localhost:8080}")
    private String universeBaseUrl;

    public RRegionService(RRegionRepository repository, KeyService keyService, JwtService jwtService, RUniverseClientService universeClientService) {
        this.repository = repository;
        this.keyService = keyService;
        this.jwtService = jwtService;
        this.universeClientService = universeClientService;
    }

    public RRegion create(String name, String apiUrl, String maintainers) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
        RRegion q = new RRegion(name, apiUrl);
        q.setMaintainers(maintainers);
        RRegion saved = repository.save(q);
        // KeyPair für Region erzeugen (Intent main-jwt-token, Owner = Regionsname)
        try {
            var keys = keyService.createECCKeys();
            KeyId keyId = KeyId.of(saved.getName(), KeyIntent.MAIN_JWT_TOKEN, UUID.randomUUID().toString());
            keyService.storeKeyPair(
                KeyType.REGION,
                keyId,
                keys
            );
            // JWT erzeugen mit privatem Region Key (Subject = regionsname)
            var privateKeyOpt = keyService.getPrivateKey(KeyType.REGION, keyId);
            if (privateKeyOpt.isPresent()) {
                String token = jwtService.createTokenWithSecretKey(privateKeyOpt.get(), saved.getName(), java.util.Map.of("region", saved.getName()), java.time.Instant.now().plusSeconds(300));
                // Universe Client konfigurieren
                universeClientService.setBaseUrl(universeBaseUrl);
                // Public Key Base64 für Registrierung
                var publicKeyOpt = keyService.getPublicKey(KeyType.REGION, keyId);
                String publicBase64 = publicKeyOpt.map(pk -> Base64.getEncoder().encodeToString(pk.getEncoded())).orElse(null);
                boolean registered = universeClientService.createRegion(token, saved.getName(), apiUrl, publicBase64, maintainers);
                if (!registered) {
                    log.warn("Region '{}' konnte im Universe nicht registriert werden", saved.getName());
                } else {
                    log.info("Region '{}' erfolgreich im Universe registriert", saved.getName());
                }
            } else {
                log.warn("Privater Schlüssel für Region '{}' nicht gefunden – Universe Registrierung übersprungen", saved.getName());
            }
        } catch (Exception e) {
            log.warn("Cannot create system auth key or register region {}: {}", saved.getName(), e.getMessage());
        }
        return saved;
    }

    // Bestehende create-Methode bleibt für Abwärtskompatibilität
    public RRegion create(String name, String apiUrl) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (apiUrl == null || apiUrl.isBlank()) throw new IllegalArgumentException("apiUrl must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("Region name exists: " + name);
        if (repository.existsByApiUrl(apiUrl)) throw new IllegalArgumentException("Region apiUrl exists: " + apiUrl);
        RRegion q = new RRegion(name, apiUrl);
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

    public List<RRegion> listAll() {
        return repository.findAll();
    }

    public RRegion update(String id, String name, String apiUrl, String maintainers) {
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
        if (maintainers != null) existing.setMaintainers(maintainers);
        return repository.save(existing);
    }

    public RRegion updateFull(String id, String name, String apiUrl, String maintainers, Boolean enabled) {
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
        if (maintainers != null) existing.setMaintainers(maintainers);
        if (enabled != null) existing.setEnabled(enabled);
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

    public Optional<String>  getRegionNameById(String regionId) {
        return repository.getRegionNameByIdAndEnabled(regionId, true);
    }

    public RRegion setEnabled(String id, boolean enabled) {
        RRegion existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
        existing.setEnabled(enabled);
        return repository.save(existing);
    }
}
