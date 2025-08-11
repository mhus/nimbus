package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.TerrainGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerrainGroupRepository extends JpaRepository<TerrainGroup, Long> {

    Optional<TerrainGroup> findByWorldAndId(String world, Long id);

    List<TerrainGroup> findByWorld(String world);
}
