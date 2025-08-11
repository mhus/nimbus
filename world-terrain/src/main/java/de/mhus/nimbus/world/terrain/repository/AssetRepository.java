package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByWorldAndName(String world, String name);

    Page<Asset> findByWorld(String world, Pageable pageable);

    void deleteByWorldAndName(String world, String name);
}
