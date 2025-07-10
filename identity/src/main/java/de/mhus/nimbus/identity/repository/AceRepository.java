package de.mhus.nimbus.identity.repository;

import de.mhus.nimbus.identity.entity.Ace;
import de.mhus.nimbus.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für Access Control Entities (ACE)
 */
@Repository
public interface AceRepository extends JpaRepository<Ace, Long> {

    /**
     * Findet alle ACEs für einen bestimmten Benutzer, sortiert nach Reihenfolge
     */
    List<Ace> findByUserOrderByOrderValueAsc(User user);

    /**
     * Findet alle aktiven ACEs für einen bestimmten Benutzer, sortiert nach Reihenfolge
     */
    List<Ace> findByUserAndActiveOrderByOrderValueAsc(User user, Boolean active);

    /**
     * Findet alle ACEs für einen bestimmten Benutzer anhand der User-ID
     */
    @Query("SELECT a FROM Ace a WHERE a.user.id = :userId ORDER BY a.orderValue ASC")
    List<Ace> findByUserIdOrderByOrderValueAsc(@Param("userId") Long userId);

    /**
     * Findet alle aktiven ACEs für einen bestimmten Benutzer anhand der User-ID
     */
    @Query("SELECT a FROM Ace a WHERE a.user.id = :userId AND a.active = :active ORDER BY a.orderValue ASC")
    List<Ace> findByUserIdAndActiveOrderByOrderValueAsc(@Param("userId") Long userId, @Param("active") Boolean active);

    /**
     * Findet ACEs anhand einer bestimmten Regel
     */
    List<Ace> findByRuleContainingIgnoreCase(String rule);

    /**
     * Findet eine ACE anhand User und Regel
     */
    Optional<Ace> findByUserAndRule(User user, String rule);

    /**
     * Findet die höchste Reihenfolge-Nummer für einen Benutzer
     */
    @Query("SELECT MAX(a.orderValue) FROM Ace a WHERE a.user.id = :userId")
    Optional<Integer> findMaxOrderValueByUserId(@Param("userId") Long userId);

    /**
     * Prüft ob eine bestimmte Reihenfolge für einen Benutzer bereits existiert
     */
    boolean existsByUserAndOrderValue(User user, Integer orderValue);

    /**
     * Löscht alle ACEs für einen bestimmten Benutzer
     */
    void deleteByUser(User user);

    /**
     * Zählt die Anzahl der ACEs für einen bestimmten Benutzer
     */
    long countByUser(User user);

    /**
     * Zählt die Anzahl der aktiven ACEs für einen bestimmten Benutzer
     */
    long countByUserAndActive(User user, Boolean active);
}
