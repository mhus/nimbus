package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Area;
import de.mhus.nimbus.generated.types.Vector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WWorldTest {

    @Test
    void builderDefaults() {
        WWorld w = WWorld.builder().worldId("terra").build();
        assertNotNull(w.getOwner());
        assertTrue(w.getOwner().isEmpty());
        assertFalse(w.isPublicFlag());
        assertNotNull(w.getEntryPoints());
        assertTrue(w.getEntryPoints().isEmpty());
    }

    @Test
    void addEntryPoints() {
        Area a = Area.builder().a(Vector3.builder().x(0).y(0).z(0).build()).b(Vector3.builder().x(10).y(0).z(10).build()).build();
        WEntryPoint ep = WEntryPoint.builder().name("spawn").area(a).build();
        WWorld w = WWorld.builder()
                .worldId("terra")
                .entryPoints(List.of(ep))
                .owner(List.of("owner1"))
                .publicFlag(true)
                .build();
        assertEquals(1, w.getEntryPoints().size());
        assertEquals("spawn", w.getEntryPoints().get(0).getName());
        assertTrue(w.isPublicFlag());
        assertEquals("owner1", w.getOwner().get(0));
    }
}
