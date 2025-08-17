package de.mhus.nimbus.worldgenerator.repository;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldGeneratorRepository extends JpaRepository<WorldGenerator, Long> {

    Optional<WorldGenerator> findByWorldId(String worldId);

    Optional<WorldGenerator> findByName(String name);

    List<WorldGenerator> findByStatus(WorldGenerator.GenerationStatus status);

    List<WorldGenerator> findByStatusIn(List<WorldGenerator.GenerationStatus> statuses);
}
