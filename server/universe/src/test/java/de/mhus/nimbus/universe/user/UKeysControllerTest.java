package de.mhus.nimbus.universe.user;

import de.mhus.nimbus.generated.dto.CreateSKeyRequest;
import de.mhus.nimbus.generated.dto.UpdateSKeyNameRequest;
import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.shared.security.KeyKind;
import de.mhus.nimbus.shared.security.KeyType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UKeysControllerTest {

    @Test
    void create_success() {
        SKeyRepository repo = Mockito.mock(SKeyRepository.class);
        UKeysController controller = new UKeysController(repo);
        CreateSKeyRequest req = new CreateSKeyRequest();
        req.setType("universe");
        req.setKind("private");
        req.setAlgorithm("EC");
        req.setName("kid");
        req.setKey("AAA");
        SKey saved = new SKey();
        saved.setId("1");
        saved.setType(KeyType.UNIVERSE);
        saved.setKind(KeyKind.PRIVATE);
        saved.setAlgorithm("EC");
        saved.setKeyId("kid");
        saved.setCreatedAt(Instant.now());
        Mockito.when(repo.save(Mockito.any())).thenReturn(saved);
        ResponseEntity<?> resp = controller.create(req);
        assertEquals(201, resp.getStatusCode().value());
    }

    @Test
    void list_filters() {
        SKeyRepository repo = Mockito.mock(SKeyRepository.class);
        UKeysController controller = new UKeysController(repo);
        SKey e = new SKey();
        e.setId("1");
        e.setType(KeyType.UNIVERSE);
        e.setKind(KeyKind.PRIVATE);
        e.setAlgorithm("EC");
        e.setKeyId("kid");
        e.setCreatedAt(Instant.now());
        Mockito.when(repo.findAll()).thenReturn(List.of(e));
        assertEquals(1, controller.list("UNIVERSE", "PRIVATE", null, null).size());
        assertEquals(0, controller.list("WORLD", null, null, null).size());
    }

    @Test
    void updateName_notFound() {
        SKeyRepository repo = Mockito.mock(SKeyRepository.class);
        UKeysController controller = new UKeysController(repo);
        Mockito.when(repo.findById("x")).thenReturn(Optional.empty());
        UpdateSKeyNameRequest req = new UpdateSKeyNameRequest();
        req.setName("newName");
        ResponseEntity<?> resp = controller.updateName("x", req);
        assertEquals(404, resp.getStatusCode().value());
    }
}
