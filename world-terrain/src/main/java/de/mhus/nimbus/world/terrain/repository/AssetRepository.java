package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.AssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    Optional<AssetEntity> findByWorldAndName(String world, String name);

    Page<AssetEntity> findByWorld(String world, Pageable pageable);

    Page<AssetEntity> findByWorldAndType(String world, String type, Pageable pageable);

    List<AssetEntity> findByWorldAndNameIn(String world, List<String> names);

    @Modifying
    @Query("UPDATE AssetEntity a SET a.compressed = :compressed, a.compressedAt = CURRENT_TIMESTAMP WHERE a.world = :world")
    void updateCompressedDataForWorld(@Param("world") String world, @Param("compressed") byte[] compressed);

    boolean existsByWorldAndName(String world, String name);

    void deleteByWorldAndName(String world, String name);
}
