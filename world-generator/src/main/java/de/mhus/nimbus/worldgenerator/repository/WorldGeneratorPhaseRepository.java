package de.mhus.nimbus.worldgenerator.repository;

import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldGeneratorPhaseRepository extends JpaRepository<WorldGeneratorPhase, Long> {

    List<WorldGeneratorPhase> findByWorldGeneratorIdOrderByPhaseOrder(Long worldGeneratorId);

    List<WorldGeneratorPhase> findByWorldGeneratorId(Long worldGeneratorId);

    List<WorldGeneratorPhase> findByStatus(WorldGeneratorPhase.PhaseStatus status);

    List<WorldGeneratorPhase> findByPhaseType(String phaseType);

    Optional<WorldGeneratorPhase> findByWorldGeneratorIdAndPhaseType(Long worldGeneratorId, String phaseType);

    List<WorldGeneratorPhase> findByWorldGeneratorIdAndStatus(Long worldGeneratorId, WorldGeneratorPhase.PhaseStatus status);
}
