package de.mhus.nimbus.registry.repository;

import de.mhus.nimbus.registry.entity.World;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for World entity operations.
 * Provides CRUD operations and custom queries for world management.
 */
@Repository
public interface WorldRepository extends JpaRepository<World, String> {

    /**
     * Find worlds by owner ID.
     * @param ownerId the owner's user ID
     * @return list of worlds owned by the user
     */
    List<World> findByOwnerId(String ownerId);

    /**
     * Find worlds by enabled status.
     * @param enabled whether the world is enabled
     * @param pageable pagination information
     * @return page of worlds with the specified enabled status
     */
    Page<World> findByEnabled(Boolean enabled, Pageable pageable);

    /**
     * Find worlds by name containing (case-insensitive).
     * @param name the name pattern to search for
     * @param pageable pagination information
     * @return page of worlds with names containing the pattern
     */
    Page<World> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find worlds by owner ID and enabled status.
     * @param ownerId the owner's user ID
     * @param enabled whether the world is enabled
     * @return list of worlds matching the criteria
     */
    List<World> findByOwnerIdAndEnabled(String ownerId, Boolean enabled);

    /**
     * Custom query to find worlds with filters.
     * @param name optional name filter
     * @param ownerId optional owner ID filter
     * @param enabled optional enabled status filter
     * @param pageable pagination information
     * @return page of worlds matching the criteria
     */
    @Query("SELECT w FROM World w WHERE " +
           "(:name IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:ownerId IS NULL OR w.ownerId = :ownerId) AND " +
           "(:enabled IS NULL OR w.enabled = :enabled)")
    Page<World> findWorldsWithFilters(@Param("name") String name,
                                     @Param("ownerId") String ownerId,
                                     @Param("enabled") Boolean enabled,
                                     Pageable pageable);
}
