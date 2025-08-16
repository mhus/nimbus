package de.mhus.nimbus.worldgenerator.repository;

import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorldGeneratorPhaseRepository extends JpaRepository<WorldGeneratorPhase, Long> {

    List<WorldGeneratorPhase> findByWorldGeneratorIdOrderByPhaseOrder(Long worldGeneratorId);

    List<WorldGeneratorPhase> findByWorldGeneratorIdAndStatus(Long worldGeneratorId, String status);

    List<WorldGeneratorPhase> findByStatus(String status);

    @Query("SELECT wgp FROM WorldGeneratorPhase wgp WHERE wgp.worldGenerator.id = :worldGeneratorId AND wgp.archived = false ORDER BY wgp.phaseOrder")
    List<WorldGeneratorPhase> findActivePhasesByWorldGeneratorId(@Param("worldGeneratorId") Long worldGeneratorId);

    @Query("SELECT wgp FROM WorldGeneratorPhase wgp WHERE wgp.processor = :processor AND wgp.status = 'PENDING' ORDER BY wgp.createdAt")
    List<WorldGeneratorPhase> findPendingPhasesByProcessor(@Param("processor") String processor);
}
