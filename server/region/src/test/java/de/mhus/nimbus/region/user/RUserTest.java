package de.mhus.nimbus.region.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import de.mhus.nimbus.shared.user.RegionRoles; // neuer Import

public class RUserTest {

    @Test
    void defaultEnabledIsTrue() {
        RUser u = new RUser();
        assertTrue(u.isEnabled(), "Neuer User sollte enabled sein");
        // Standardrolle PLAYER sollte gesetzt werden nach Service-Create, hier Konstruktor alleine setzt keine Rolle
        assertTrue(u.getRoles().isEmpty(), "Direkter Konstruktor setzt keine Rolle");
    }

    @Test
    void disableAndEnableWorks() {
        RUser u = new RUser();
        u.disable();
        assertFalse(u.isEnabled(), "Nach disable sollte enabled=false sein");
        u.enable();
        assertTrue(u.isEnabled(), "Nach enable sollte enabled=true sein");
    }

    @Test
    void rolesRoundtrip() {
        RUser u = new RUser();
        assertTrue(u.getRoles().isEmpty());
        u.addRole(RegionRoles.PLAYER);
        assertTrue(u.hasRole(RegionRoles.PLAYER));
        u.addRole(RegionRoles.ADMIN);
        assertTrue(u.hasRole(RegionRoles.ADMIN));
        assertEquals(2, u.getRoles().size());
        u.removeRole(RegionRoles.PLAYER);
        assertFalse(u.hasRole(RegionRoles.PLAYER));
        assertEquals(1, u.getRoles().size());
    }

    @Test
    void nullEnabledTreatedAsTrue() {
        RUser u = new RUser();
        u.setEnabled(null); // simuliert alten Datensatz ohne Feld
        assertTrue(u.isEnabled(), "Null enabled soll als true interpretiert werden");
    }

    @Test
    void setRolesRawEmptyClearsRoles() {
        RUser u = new RUser();
        u.addRole(RegionRoles.PLAYER);
        assertFalse(u.getRoles().isEmpty());
        u.setRolesRaw(" ");
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    void setRolesRawParsesValidAndIgnoresInvalid() {
        RUser u = new RUser();
        u.setRolesRaw("ADMIN,INVALID,EDITOR,ADMIN");
        assertTrue(u.hasRole(RegionRoles.ADMIN));
        assertTrue(u.hasRole(RegionRoles.EDITOR));
        assertFalse(u.hasRole(RegionRoles.PLAYER));
        assertEquals(2, u.getRoles().size(), "Duplikate und ungültige Rollen sollten entfernt/ignoriert sein");
    }
}
