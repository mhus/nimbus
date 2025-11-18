package de.mhus.nimbus.universe.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.mhus.nimbus.shared.security.Roles;

class UUserRolesTest {

    @Test
    void newUser_shouldHaveNoRoles() {
        UUser u = new UUser("name","mail@example.com");
        assertThat(u.getRoles()).isEmpty();
        assertThat(u.getRolesRaw()).isNull();
    }

    @Test
    void setRoles_varargs_shouldStoreDistinctCommaSeparated() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(Roles.USER, Roles.ADMIN, Roles.USER);
        assertThat(u.getRoles()).containsExactly(Roles.USER, Roles.ADMIN); // Einfügereihenfolge / LinkedHashSet
        assertThat(u.getRolesRaw()).isEqualTo("USER,ADMIN");
    }

    @Test
    void addRole_shouldAddAndNotDuplicate() {
        UUser u = new UUser("name","mail@example.com");
        boolean first = u.addRole(Roles.USER);
        boolean second = u.addRole(Roles.USER);
        boolean third = u.addRole(Roles.ADMIN);
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isTrue();
        assertThat(u.getRoles()).containsExactly(Roles.USER, Roles.ADMIN);
        assertThat(u.hasRole(Roles.ADMIN)).isTrue();
    }

    @Test
    void removeRole_shouldRemoveIfPresent() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(Roles.USER, Roles.ADMIN);
        boolean removedUser = u.removeRole(Roles.USER);
        boolean removedUserAgain = u.removeRole(Roles.USER);
        boolean removedAdmin = u.removeRole(Roles.ADMIN);
        assertThat(removedUser).isTrue();
        assertThat(removedUserAgain).isFalse();
        assertThat(removedAdmin).isTrue();
        assertThat(u.getRoles()).isEmpty();
        assertThat(u.getRolesRaw()).isNull(); // nach Entfernen aller Rollen wird null gesetzt
    }

    @Test
    void setRoles_setShouldPersistOrderDistinct() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(Set.of(Roles.ADMIN, Roles.USER));
        // Reihenfolge in Set.of ist nicht garantiert -> wir testen nur Inhalt (LinkedHashSet Einsatz in getRoles liefert Insert-Reihenfolge aus Raw String)
        assertThat(u.getRoles()).containsExactlyInAnyOrder(Roles.USER, Roles.ADMIN);
        assertThat(u.getRolesRaw()).contains("USER").contains("ADMIN");
    }

    @Test
    void setRolesRaw_shouldTrimAndAllowDirectParsing() {
        UUser u = new UUser("name","mail@example.com");
        u.setRolesRaw(" USER , ADMIN ");
        assertThat(u.getRoles()).containsExactly(Roles.USER, Roles.ADMIN);
        assertThat(u.hasRole(Roles.USER)).isTrue();
        assertThat(u.hasRole(Roles.ADMIN)).isTrue();
    }

    @Test
    void clearRoles_viaEmptyVarargs_shouldNullRaw() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(Roles.USER);
        u.setRoles(); // leer
        assertThat(u.getRoles()).isEmpty();
        assertThat(u.getRolesRaw()).isNull();
    }
}

