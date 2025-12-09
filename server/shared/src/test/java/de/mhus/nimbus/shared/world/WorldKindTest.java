package de.mhus.nimbus.shared.world;

import static org.junit.jupiter.api.Assertions.*;

import de.mhus.nimbus.shared.types.WorldKind;
import org.junit.jupiter.api.Test;

class WorldKindTest {

    @Test
    void parseWorldOnly() {
        WorldKind wk = WorldKind.of("earth");
        assertEquals("earth", wk.worldId());
        assertFalse(wk.hasZone());
        assertFalse(wk.hasBranch());
        assertTrue(wk.isMain());
        assertEquals("earth", wk.toString());
    }

    @Test
    void parseWorldZone() {
        WorldKind wk = WorldKind.of("earth$europe");
        assertEquals("earth", wk.worldId());
        assertEquals("europe", wk.zone());
        assertTrue(wk.hasZone());
        assertFalse(wk.hasBranch());
        assertTrue(wk.isZone());
        assertEquals("earth$europe", wk.toString());
    }

    @Test
    void parseWorldBranch() {
        WorldKind wk = WorldKind.of("earth:main");
        assertEquals("earth", wk.worldId());
        assertEquals("main", wk.branch());
        assertFalse(wk.hasZone());
        assertTrue(wk.hasBranch());
        assertTrue(wk.isBranch());
        assertEquals("earth:main", wk.toString());
    }

    @Test
    void parseWorldZoneBranch() {
        WorldKind wk = WorldKind.of("earth$europe:dev-feature");
        assertEquals("earth", wk.worldId());
        assertEquals("europe", wk.zone());
        assertEquals("dev-feature", wk.branch());
        assertTrue(wk.hasZone());
        assertTrue(wk.hasBranch());
        assertTrue(wk.isBranch());
        assertEquals("earth$europe:dev-feature", wk.toString());
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> WorldKind.of(" "));
        assertThrows(IllegalArgumentException.class, () -> WorldKind.of(null));
    }

    @Test
    void rejectsInvalidWorld() {
        assertThrows(IllegalArgumentException.class, () -> WorldKind.of("earth!bad"));
    }

    @Test
    void rejectsInvalidZone() {
        assertThrows(IllegalArgumentException.class, () -> WorldKind.of("earth$euro+pe"));
    }

    @Test
    void rejectsInvalidBranch() {
        assertThrows(IllegalArgumentException.class, () -> WorldKind.of("earth$europe:main*branch"));
    }

    @Test
    void ignoresEmptyBranchAfterColon() {
        WorldKind wk = WorldKind.of("earth$europe:");
        assertEquals("earth", wk.worldId());
        assertEquals("europe", wk.zone());
        assertFalse(wk.hasBranch());
        assertEquals("earth$europe", wk.toString());
    }

    @Test
    void equalsAndHashCode() {
        WorldKind a = WorldKind.of("earth$europe:main");
        WorldKind b = WorldKind.of("earth$europe:main");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void convenienceFlagsMain() {
        WorldKind wk = WorldKind.of("earth");
        assertTrue(wk.isMain());
        assertFalse(wk.isZone());
        assertFalse(wk.isBranch());
    }

    @Test
    void convenienceFlagsZone() {
        WorldKind wk = WorldKind.of("earth$europe");
        assertFalse(wk.isMain());
        assertTrue(wk.isZone());
        assertFalse(wk.isBranch());
    }

    @Test
    void convenienceFlagsBranchWithoutZone() {
        WorldKind wk = WorldKind.of("earth:main");
        assertFalse(wk.isMain());
        assertFalse(wk.isZone());
        assertTrue(wk.isBranch());
    }

    @Test
    void convenienceFlagsBranchWithZone() {
        WorldKind wk = WorldKind.of("earth$europe:main");
        assertFalse(wk.isMain());
        assertFalse(wk.isZone());
        assertTrue(wk.isBranch());
    }
}
