package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.MaterialEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<MaterialEntity, Integer> {
    Optional<MaterialEntity> findByName(String name);
    Page<MaterialEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
