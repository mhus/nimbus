package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.SAssetService;
import de.mhus.nimbus.world.shared.world.SAsset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EAssetControllerTest {

    @Test
    void testGetNotFound() {
        SAssetService service = Mockito.mock(SAssetService.class);
        Mockito.when(service.findByPath(WorldId.unchecked("reg1:world1"), "path/file.txt")).thenReturn(Optional.empty());
        EAssetController ctrl = new EAssetController(service);
        ResponseEntity<?> resp = ctrl.get("reg1", "reg1:world1", "path/file.txt");
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void testGetFound() {
        SAssetService service = Mockito.mock(SAssetService.class);
        SAsset asset = new SAsset();
        asset.setId("X1");
        asset.setWorldId("world1");
        asset.setPath("path/file.txt");
        asset.setName("file.txt");
        asset.setCreatedAt(Instant.now());
        asset.setCreatedBy("editor");
        asset.setEnabled(true);
        Mockito.when(service.findByPath(WorldId.unchecked("reg1:world1"), "path/file.txt")).thenReturn(Optional.of(asset));
        EAssetController ctrl = new EAssetController(service);
        ResponseEntity<?> resp = ctrl.get("reg1", "reg1:world1", "path/file.txt");
        assertEquals(200, resp.getStatusCode().value());
    }
}

