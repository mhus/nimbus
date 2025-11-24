package de.mhus.nimbus.universe.user;

import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.universe.security.Role;
import de.mhus.nimbus.shared.user.UniverseRoles;
import de.mhus.nimbus.generated.dto.CreateSKeyRequest; // geändert
import de.mhus.nimbus.generated.dto.SKeyDto; // geändert
import de.mhus.nimbus.generated.dto.UpdateSKeyNameRequest; // geändert
import de.mhus.nimbus.shared.security.KeyKind;
import de.mhus.nimbus.shared.security.KeyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping(UKeysController.BASE_PATH)
@Role({UniverseRoles.ADMIN})
@Validated
@Tag(name = "Keys", description = "Verwaltung von Schlüsseln im Universe-Modul")
public class UKeysController {

    public static final String BASE_PATH = "/universe/user/keys";

    private final SKeyRepository repository;

    public UKeysController(SKeyRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------
    // List/Search
    @GetMapping
    @Operation(summary = "Schlüssel auflisten/suchen", description = "Listet Schlüssel auf. Optional per Query-Param filterbar.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste der Schlüssel (ohne Key-Material)")
    })
    public List<SKeyDto> list(
            @Parameter(description = "Filter: type") @RequestParam(name = "type", required = false) String type,
            @Parameter(description = "Filter: kind") @RequestParam(name = "kind", required = false) String kind,
            @Parameter(description = "Filter: name") @RequestParam(name = "name", required = false) String name,
            @Parameter(description = "Filter: algorithm") @RequestParam(name = "algorithm", required = false) String algorithm
    ) {
        Iterable<SKey> all = repository.findAll();
        List<SKeyDto> out = new ArrayList<>();
        StreamSupport.stream(all.spliterator(), false)
                .filter(e -> type == null || (e.getType() != null && type.equalsIgnoreCase(e.getType().name())))
                .filter(e -> kind == null || (e.getKind() != null && kind.equalsIgnoreCase(e.getKind().name())))
                .filter(e -> name == null || name.equals(e.getKeyId()))
                .filter(e -> algorithm == null || algorithm.equals(e.getAlgorithm()))
                .map(this::toDto)
                .forEach(out::add);
        return out;
    }

    // ------------------------------------------------------------
    // Create
    @PostMapping
    @Operation(summary = "Schlüssel erstellen")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Schlüssel erstellt"),
        @ApiResponse(responseCode = "400", description = "Validierungsfehler")
    })
    public ResponseEntity<SKeyDto> create(@Valid @RequestBody CreateSKeyRequest req) {
        String rawType = req.getType();
        String rawKind = req.getKind();
        if (rawType == null || rawKind == null) return ResponseEntity.badRequest().build();
        KeyType keyType;
        KeyKind keyKind;
        try {
            keyType = KeyType.valueOf(rawType.trim().toUpperCase());
            keyKind = KeyKind.valueOf(rawKind.trim().toUpperCase());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
        SKey e = new SKey();
        e.setType(keyType);
        e.setKind(keyKind);
        e.setAlgorithm(req.getAlgorithm());
        e.setKeyId(req.getName());
        e.setKey(req.getKey()); // wird nie zurückgegeben
        SKey saved = repository.save(e);
        SKeyDto dto = toDto(saved);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + saved.getId())).body(dto);
    }

    // ------------------------------------------------------------
    // Update name only
    @PutMapping("/{id}")
    @Operation(summary = "Schlüsselnamen ändern", description = "Ändert ausschließlich das Feld 'name'.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Aktualisierte Metadaten"),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<SKeyDto> updateName(@PathVariable("id") String id, @Valid @RequestBody UpdateSKeyNameRequest req) {
        Optional<SKey> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SKey e = opt.get();
        e.setKeyId(req.getName());
        SKey saved = repository.save(e);
        return ResponseEntity.ok(toDto(saved));
    }

    // ------------------------------------------------------------
    // Delete
    @DeleteMapping("/{id}")
    @Operation(summary = "Schlüssel löschen")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Gelöscht"),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------
    private SKeyDto toDto(SKey e) {
        return SKeyDto.builder()
                .id(e.getId())
                .type(e.getType() != null ? e.getType().name() : null)
                .kind(e.getKind() != null ? e.getKind().name() : null)
                .algorithm(e.getAlgorithm())
                .keyId(e.getKeyId())
                .owner(e.getOwner())
                .intent(e.getIntent())
                .createdAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString())
                .build();
    }
}
