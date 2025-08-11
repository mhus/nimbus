package de.mhus.nimbus.world.terrain.repository;

import de.mhus.nimbus.world.terrain.entity.WorldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldRepository extends JpaRepository<WorldEntity, String> {
    Optional<WorldEntity> findByName(String name);
}
