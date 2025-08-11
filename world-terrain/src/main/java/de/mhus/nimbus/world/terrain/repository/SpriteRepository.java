package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.Sprite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpriteRepository extends JpaRepository<Sprite, String> {

    @Query("SELECT s FROM Sprite s WHERE s.world = :world AND s.level = :level AND s.enabled = true " +
           "AND ((s.clusterX0 = :clusterX AND s.clusterY0 = :clusterY) " +
           "OR (s.clusterX1 = :clusterX AND s.clusterY1 = :clusterY) " +
           "OR (s.clusterX2 = :clusterX AND s.clusterY2 = :clusterY) " +
           "OR (s.clusterX3 = :clusterX AND s.clusterY3 = :clusterY))")
    List<Sprite> findByCluster(@Param("world") String world,
                              @Param("level") Integer level,
                              @Param("clusterX") Integer clusterX,
                              @Param("clusterY") Integer clusterY);
}
