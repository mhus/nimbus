package de.mhus.nimbus.universe.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.mhus.nimbus.shared.user.UniverseRoles; // geändert

class UUserRolesTest {

    @Test
    void newUser_shouldHaveNoRoles() {
        UUser u = new UUser("name","mail@example.com");
        assertThat(u.getRoles()).isEmpty();
        assertThat(u.getRolesAsString()).isEmpty();
    }

    @Test
    void setRoles_varargs_shouldStoreDistinctCommaSeparated() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(UniverseRoles.USER, UniverseRoles.ADMIN, UniverseRoles.USER);
        assertThat(u.getRoles()).containsExactly(UniverseRoles.USER, UniverseRoles.ADMIN);
        assertThat(u.getRolesAsString()).isEqualTo("USER,ADMIN");
    }

    @Test
    void addRole_shouldAddAndNotDuplicate() {
        UUser u = new UUser("name","mail@example.com");
        boolean first = u.addRole(UniverseRoles.USER);
        boolean second = u.addRole(UniverseRoles.USER);
        boolean third = u.addRole(UniverseRoles.ADMIN);
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isTrue();
        assertThat(u.getRoles()).containsExactly(UniverseRoles.USER, UniverseRoles.ADMIN);
        assertThat(u.hasRole(UniverseRoles.ADMIN)).isTrue();
    }

    @Test
    void removeRole_shouldRemoveIfPresent() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(UniverseRoles.USER, UniverseRoles.ADMIN);
        boolean removedUser = u.removeRole(UniverseRoles.USER);
        boolean removedUserAgain = u.removeRole(UniverseRoles.USER);
        boolean removedAdmin = u.removeRole(UniverseRoles.ADMIN);
        assertThat(removedUser).isTrue();
        assertThat(removedUserAgain).isFalse();
        assertThat(removedAdmin).isTrue();
        assertThat(u.getRoles()).isEmpty();
        assertThat(u.getRolesAsString()).isEmpty();
    }

    @Test
    void setRoles_setShouldPersistOrderDistinct() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(Set.of(UniverseRoles.ADMIN, UniverseRoles.USER));
        assertThat(u.getRoles()).containsExactlyInAnyOrder(UniverseRoles.USER, UniverseRoles.ADMIN);
        assertThat(u.getRolesAsString()).contains("USER").contains("ADMIN");
    }

    @Test
    void setRolesRaw_shouldTrimAndAllowDirectParsing() {
        UUser u = new UUser("name","mail@example.com");
        u.setRolesStringList(" USER , ADMIN ");
        assertThat(u.getRoles()).containsExactly(UniverseRoles.USER, UniverseRoles.ADMIN);
        assertThat(u.hasRole(UniverseRoles.USER)).isTrue();
        assertThat(u.hasRole(UniverseRoles.ADMIN)).isTrue();
    }

    @Test
    void clearRoles_viaEmptyVarargs_shouldNullRaw() {
        UUser u = new UUser("name","mail@example.com");
        u.setRoles(UniverseRoles.USER);
        u.setRoles();
        assertThat(u.getRoles()).isEmpty();
        assertThat(u.getRolesAsString()).isEmpty();
    }
}
