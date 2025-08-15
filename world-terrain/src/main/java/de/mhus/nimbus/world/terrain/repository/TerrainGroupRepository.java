package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.TerrainGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerrainGroupRepository extends JpaRepository<TerrainGroupEntity, Long> {

    Optional<TerrainGroupEntity> findByWorldAndGroupId(String world, Long groupId);

    List<TerrainGroupEntity> findByWorld(String world);

    List<TerrainGroupEntity> findByWorldAndType(String world, String type);

    boolean existsByWorldAndGroupId(String world, Long groupId);

    void deleteByWorldAndGroupId(String world, Long groupId);
}
