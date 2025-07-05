package de.mhus.nimbus.registry.repository;

import de.mhus.nimbus.registry.entity.World;
import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.WorldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository für World-Entitäten
 */
@Repository
public interface WorldRepository extends JpaRepository<World, Long> {

    /**
     * Findet eine Welt nach worldId
     */
    Optional<World> findByWorldId(String worldId);

    /**
     * Findet Welten nach Planet-Name und Environment
     */
    @Query("SELECT w FROM World w JOIN w.planet p WHERE p.name = :planetName AND p.environment = :environment AND p.active = true")
    List<World> findByPlanetNameAndEnvironment(@Param("planetName") String planetName, @Param("environment") Environment environment);

    /**
     * Findet eine spezifische Welt auf einem Planeten
     */
    @Query("SELECT w FROM World w JOIN w.planet p WHERE p.name = :planetName AND w.name = :worldName AND p.environment = :environment AND p.active = true")
    Optional<World> findByPlanetNameAndWorldNameAndEnvironment(@Param("planetName") String planetName,
                                                               @Param("worldName") String worldName,
                                                               @Param("environment") Environment environment);

    /**
     * Findet Welten nach Status
     */
    List<World> findByStatusOrderByName(WorldStatus status);

    /**
     * Findet aktive Welten eines Planeten
     */
    @Query("SELECT w FROM World w JOIN w.planet p WHERE p.name = :planetName AND p.environment = :environment AND w.status = 'ACTIVE' AND p.active = true")
    List<World> findActiveWorldsByPlanetNameAndEnvironment(@Param("planetName") String planetName, @Param("environment") Environment environment);

    /**
     * Überprüft ob eine Welt existiert
     */
    @Query("SELECT COUNT(w) > 0 FROM World w JOIN w.planet p WHERE p.name = :planetName AND w.name = :worldName AND p.environment = :environment AND p.active = true")
    boolean existsByPlanetNameAndWorldNameAndEnvironment(@Param("planetName") String planetName,
                                                        @Param("worldName") String worldName,
                                                        @Param("environment") Environment environment);

    /**
     * Findet Welten, die seit einer bestimmten Zeit nicht mehr gecheckt wurden
     */
    @Query("SELECT w FROM World w WHERE w.lastHealthCheck < :threshold")
    List<World> findWorldsWithStaleHealthCheck(@Param("threshold") Instant threshold);

    /**
     * Findet Welten nach Metadaten-Key und Value
     */
    @Query("SELECT w FROM World w JOIN w.metadata m WHERE KEY(m) = :key AND VALUE(m) = :value")
    List<World> findByMetadata(@Param("key") String key, @Param("value") String value);

    /**
     * Zählt Welten nach Status gruppiert
     */
    @Query("SELECT w.status, COUNT(w) FROM World w GROUP BY w.status")
    List<Object[]> countWorldsByStatus();
}
