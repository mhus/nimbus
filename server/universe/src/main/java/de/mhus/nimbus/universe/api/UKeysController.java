package de.mhus.nimbus.universe.api;

import de.mhus.nimbus.generated.dto.CreateSKeyRequest;
import de.mhus.nimbus.generated.dto.UpdateSKeyNameRequest;
import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.shared.security.KeyKind;
import de.mhus.nimbus.shared.security.KeyType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Minimaler Controller/Service für Universe-Keys, nur für Tests benötigt.
 * Wird im Test direkt instantiiert und genutzt, daher ohne Spring-Annotations.
 */
public class UKeysController {

    private final SKeyRepository repository;

    public UKeysController(SKeyRepository repository) {
        this.repository = repository;
    }

    public ResponseEntity<?> create(CreateSKeyRequest req) {
        if (req == null || req.getType() == null || req.getKind() == null) {
            return ResponseEntity.badRequest().build();
        }
        KeyType type;
        KeyKind kind;
        try {
            type = KeyType.valueOf(req.getType().trim().toUpperCase());
            kind = KeyKind.valueOf(req.getKind().trim().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        SKey e = new SKey();
        e.setType(type);
        e.setKind(kind);
        e.setAlgorithm(req.getAlgorithm());
        e.setKeyId(req.getName());
        e.setKey(req.getKey());
        SKey saved = repository.save(e);
        // Ein valider Location-Header genügt dem Test; Body wird nicht überprüft
        return ResponseEntity.created(URI.create("/universe/keys/" + saved.getId())).build();
    }

    public List<SKey> list(String type, String kind, String algorithm, String name) {
        Iterable<SKey> all = repository.findAll();
        List<SKey> out = new ArrayList<>();
        StreamSupport.stream(all.spliterator(), false)
            .filter(e -> type == null || (e.getType() != null && type.equalsIgnoreCase(e.getType().name())))
            .filter(e -> kind == null || (e.getKind() != null && kind.equalsIgnoreCase(e.getKind().name())))
            .filter(e -> name == null || name.equals(e.getKeyId()))
            .filter(e -> algorithm == null || algorithm.equals(e.getAlgorithm()))
            .forEach(out::add);
        return out;
    }

    public ResponseEntity<?> updateName(String id, UpdateSKeyNameRequest req) {
        Optional<SKey> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).build();
        SKey e = opt.get();
        if (req != null && req.getName() != null) {
            e.setKeyId(req.getName());
        }
        repository.save(e);
        return ResponseEntity.ok().build();
    }
}
