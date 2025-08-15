package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.SpriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpriteRepository extends JpaRepository<SpriteEntity, String> {

    List<SpriteEntity> findByWorldAndLevelAndEnabledAndClusterX0AndClusterY0(
        String world, Integer level, Boolean enabled, Integer clusterX0, Integer clusterY0);

    @Query("SELECT s FROM SpriteEntity s WHERE s.world = ?1 AND s.level = ?2 AND s.enabled = ?3 AND " +
           "((s.clusterX0 = ?4 AND s.clusterY0 = ?5) OR " +
           "(s.clusterX1 = ?4 AND s.clusterY1 = ?5) OR " +
           "(s.clusterX2 = ?4 AND s.clusterY2 = ?5) OR " +
           "(s.clusterX3 = ?4 AND s.clusterY3 = ?5))")
    List<SpriteEntity> findSpritesInCluster(
        String world, Integer level, Boolean enabled, Integer clusterX, Integer clusterY);

    List<SpriteEntity> findByWorldAndLevel(String world, Integer level);
}
