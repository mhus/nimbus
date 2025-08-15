package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.MapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MapRepository extends JpaRepository<MapEntity, Long> {

    Optional<MapEntity> findByWorldAndLevelAndClusterXAndClusterY(
        String world, Integer level, Integer clusterX, Integer clusterY);

    List<MapEntity> findByWorldAndLevel(String world, Integer level);

    @Query("SELECT m FROM MapEntity m WHERE m.world = ?1 AND m.level = ?2 AND " +
           "(m.clusterX, m.clusterY) IN ?3")
    List<MapEntity> findByWorldAndLevelAndClusterCoordinates(
        String world, Integer level, List<Object[]> coordinates);

    void deleteByWorldAndLevel(String world, Integer level);

    void deleteByWorldAndLevelAndClusterXAndClusterY(
        String world, Integer level, Integer clusterX, Integer clusterY);
}
