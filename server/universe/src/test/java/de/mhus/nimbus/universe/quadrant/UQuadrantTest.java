package de.mhus.nimbus.universe.quadrant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

class UQuadrantTest {

    @Test
    void testSetMaintainersCsvNormalization() {
        UQuadrant q = new UQuadrant("name","url","key");
        q.setMaintainers(" u1 , u2 ,u2,, u3 ");
        assertEquals(Set.of("u1","u2","u3"), q.getMaintainerSet(), "CSV sollte normalisiert und Duplikate entfernt werden");
        q.setMaintainers("   ");
        assertTrue(q.getMaintainerSet().isEmpty(), "Leer/Blank sollte leeres Set liefern");
    }

    @Test
    void testAddRemoveMaintainer() {
        UQuadrant q = new UQuadrant("name","url","key");
        q.addMaintainer("u1");
        q.addMaintainer("u2");
        q.addMaintainer("u2"); // Duplikat
        assertEquals(Set.of("u1","u2"), q.getMaintainerSet());
        assertTrue(q.hasMaintainer("u1"));
        q.removeMaintainer("u1");
        assertFalse(q.hasMaintainer("u1"));
        assertEquals(Set.of("u2"), q.getMaintainerSet());
    }

    @Test
    void testConstructorWithSet() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new UQuadrant("id","name","url","key", Set.of("x","y","x"))
        );
        assertTrue(ex.getMessage().startsWith("duplicate element"), "Exception-Meldung sollte Duplikat anzeigen");
    }
}
