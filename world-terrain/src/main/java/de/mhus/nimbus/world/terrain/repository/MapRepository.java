package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.MapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MapRepository extends JpaRepository<MapEntity, Long> {

    Optional<MapEntity> findByWorldAndLevelAndClusterXAndClusterY(
        String world, Integer level, Integer clusterX, Integer clusterY);

    List<MapEntity> findByWorldAndLevel(String world, Integer level);

    @Query("DELETE FROM MapEntity m WHERE m.world = :world AND m.level = :level")
    void deleteByWorldAndLevel(@Param("world") String world, @Param("level") Integer level);

    @Query("SELECT m FROM MapEntity m WHERE m.world = :world AND m.level = :level " +
           "AND m.clusterX IN :clusterXs AND m.clusterY IN :clusterYs")
    List<MapEntity> findByWorldAndLevelAndClusterCoordinates(
        @Param("world") String world,
        @Param("level") Integer level,
        @Param("clusterXs") List<Integer> clusterXs,
        @Param("clusterYs") List<Integer> clusterYs);
}
