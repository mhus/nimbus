package de.mhus.nimbus.identity.repository;

import de.mhus.nimbus.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository für User-Entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Findet einen Benutzer anhand des Benutzernamens
     */
    Optional<User> findByUsername(String username);

    /**
     * Findet einen Benutzer anhand der E-Mail-Adresse
     */
    Optional<User> findByEmail(String email);

    /**
     * Prüft ob ein Benutzername bereits existiert
     */
    boolean existsByUsername(String username);

    /**
     * Prüft ob eine E-Mail-Adresse bereits existiert
     */
    boolean existsByEmail(String email);

    /**
     * Findet aktive Benutzer anhand des Benutzernamens
     */
    Optional<User> findByUsernameAndActiveTrue(String username);
}
