package de.mhus.nimbus.region.user;

import de.mhus.nimbus.shared.asset.SAssetService;
import de.mhus.nimbus.shared.persistence.SAsset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RAssetControllerTest {

    @Test
    void testGetNotFound() {
        SAssetService service = Mockito.mock(SAssetService.class);
        Mockito.when(service.findByPath("r1", null, "file.txt")).thenReturn(Optional.empty());
        RAssetController ctrl = new RAssetController(service);
        ResponseEntity<?> resp = ctrl.get("r1", "file.txt", null, false);
        assertEquals(404, resp.getStatusCodeValue());
    }

    @Test
    void testGetFound() {
        SAssetService service = Mockito.mock(SAssetService.class);
        SAsset asset = new SAsset();
        asset.setId("A1");
        asset.setRegionId("r1");
        asset.setPath("file.txt");
        asset.setName("file.txt");
        asset.setCreatedAt(Instant.now());
        asset.setCreatedBy("tester");
        asset.setEnabled(true);
        Mockito.when(service.findByPath("r1", null, "file.txt")).thenReturn(Optional.of(asset));
        RAssetController ctrl = new RAssetController(service);
        ResponseEntity<?> resp = ctrl.get("r1", "file.txt", null, false);
        assertEquals(200, resp.getStatusCodeValue());
    }
}

