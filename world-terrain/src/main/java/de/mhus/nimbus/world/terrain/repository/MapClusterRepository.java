package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.MapCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MapClusterRepository extends JpaRepository<MapCluster, Long> {

    Optional<MapCluster> findByWorldAndLevelAndClusterXAndClusterY(
        String world, Integer level, Integer clusterX, Integer clusterY);

    List<MapCluster> findByWorldAndLevel(String world, Integer level);

    void deleteByWorldAndLevel(String world, Integer level);
}
