package de.mhus.nimbus.registry.repository;

import de.mhus.nimbus.registry.entity.Planet;
import de.mhus.nimbus.shared.avro.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für Planet-Entitäten
 */
@Repository
public interface PlanetRepository extends JpaRepository<Planet, Long> {

    /**
     * Findet einen Planeten nach Name und Environment
     */
    Optional<Planet> findByNameIgnoreCaseAndEnvironmentAndActiveTrue(String name, Environment environment);

    /**
     * Findet alle aktiven Planeten in einem Environment
     */
    List<Planet> findByEnvironmentAndActiveTrueOrderByName(Environment environment);

    /**
     * Findet Planeten nach Name-Pattern (case-insensitive)
     */
    List<Planet> findByNameContainingIgnoreCaseAndEnvironmentAndActiveTrueOrderByName(String namePattern, Environment environment);

    /**
     * Überprüft ob ein Planet existiert
     */
    boolean existsByNameIgnoreCaseAndEnvironmentAndActiveTrue(String name, Environment environment);

    /**
     * Findet Planeten mit ihren Welten (Eager Loading)
     */
    @Query("SELECT p FROM Planet p LEFT JOIN FETCH p.worlds w WHERE p.name = :name AND p.environment = :environment AND p.active = true")
    Optional<Planet> findByNameAndEnvironmentWithWorlds(@Param("name") String name, @Param("environment") Environment environment);

    /**
     * Findet alle Planeten mit aktiven Welten
     */
    @Query("SELECT DISTINCT p FROM Planet p JOIN p.worlds w WHERE p.environment = :environment AND p.active = true AND w.status = 'ACTIVE'")
    List<Planet> findPlanetsWithActiveWorlds(@Param("environment") Environment environment);

    /**
     * Zählt die Anzahl der Welten pro Planet
     */
    @Query("SELECT p.name, COUNT(w) FROM Planet p LEFT JOIN p.worlds w WHERE p.environment = :environment AND p.active = true GROUP BY p.name")
    List<Object[]> countWorldsPerPlanet(@Param("environment") Environment environment);
}
