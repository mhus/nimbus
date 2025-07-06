package de.mhus.nimbus.worldlife.repository;

import de.mhus.nimbus.shared.character.CharacterType;
import de.mhus.nimbus.shared.dto.CharacterOperationMessage;
import de.mhus.nimbus.worldlife.entity.WorldCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WorldCharacter entities
 */
@Repository
public interface WorldCharacterRepository extends JpaRepository<WorldCharacter, Long> {

    /**
     * Find a character by world ID and position
     */
    Optional<WorldCharacter> findByWorldIdAndXAndYAndZ(String worldId, double x, double y, double z);

    /**
     * Find all characters in a specific world
     */
    List<WorldCharacter> findByWorldId(String worldId);

    /**
     * Find all characters by type in a specific world
     */
    List<WorldCharacter> findByWorldIdAndCharacterType(String worldId, CharacterType characterType);

    /**
     * Find all active characters in a world
     */
    List<WorldCharacter> findByWorldIdAndActiveTrue(String worldId);

    /**
     * Find all characters by name (case-insensitive)
     */
    List<WorldCharacter> findByNameContainingIgnoreCase(String name);

    /**
     * Find characters within a coordinate range
     */
    @Query("SELECT wc FROM WorldCharacter wc WHERE wc.worldId = :worldId " +
           "AND wc.x BETWEEN :minX AND :maxX " +
           "AND wc.y BETWEEN :minY AND :maxY " +
           "AND wc.z BETWEEN :minZ AND :maxZ")
    List<WorldCharacter> findByWorldIdAndXBetweenAndYBetweenAndZBetween(
        @Param("worldId") String worldId,
        @Param("minX") double minX, @Param("maxX") double maxX,
        @Param("minY") double minY, @Param("maxY") double maxY,
        @Param("minZ") double minZ, @Param("maxZ") double maxZ);

    /**
     * Find characters within radius of a point
     */
    @Query("SELECT wc FROM WorldCharacter wc WHERE wc.worldId = :worldId " +
           "AND SQRT(POWER(wc.x - :centerX, 2) + POWER(wc.y - :centerY, 2) + POWER(wc.z - :centerZ, 2)) <= :radius")
    List<WorldCharacter> findByWorldIdAndWithinRadius(
            @Param("worldId") String worldId,
            @Param("centerX") double centerX,
            @Param("centerY") double centerY,
            @Param("centerZ") double centerZ,
            @Param("radius") double radius);

    /**
     * Find characters created after a specific timestamp
     */
    List<WorldCharacter> findByWorldIdAndCreatedAtAfter(String worldId, LocalDateTime timestamp);

    /**
     * Find characters modified after a specific timestamp
     */
    List<WorldCharacter> findByWorldIdAndLastModifiedAfter(String worldId, LocalDateTime timestamp);

    /**
     * Count characters by world and character type
     */
    long countByWorldIdAndCharacterType(String worldId, CharacterType characterType);

    /**
     * Count active characters in a world
     */
    long countByWorldIdAndActiveTrue(String worldId);

    /**
     * Count all characters in a world
     */
    long countByWorldId(String worldId);

    /**
     * Delete all characters in a world
     */
    void deleteByWorldId(String worldId);

    /**
     * Delete characters by type in a world
     */
    void deleteByWorldIdAndCharacterType(String worldId, CharacterType characterType);

    /**
     * Check if a character exists at the given coordinates
     */
    boolean existsByWorldIdAndXAndYAndZ(String worldId, double x, double y, double z);
}
