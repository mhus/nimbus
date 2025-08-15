package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    Optional<AssetEntity> findByWorldAndName(String world, String name);

    List<AssetEntity> findByWorld(String world);

    List<AssetEntity> findByWorldAndType(String world, String type);

    boolean existsByWorldAndName(String world, String name);

    void deleteByWorldAndName(String world, String name);
}
