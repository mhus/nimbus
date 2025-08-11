package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {
    Page<Material> findAll(Pageable pageable);
}
