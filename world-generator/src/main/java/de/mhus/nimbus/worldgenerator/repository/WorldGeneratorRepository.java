package de.mhus.nimbus.worldgenerator.repository;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldGeneratorRepository extends JpaRepository<WorldGenerator, Long> {

    Optional<WorldGenerator> findByName(String name);

    List<WorldGenerator> findByStatus(String status);

    @Query("SELECT wg FROM WorldGenerator wg WHERE wg.status = :status ORDER BY wg.createdAt DESC")
    List<WorldGenerator> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("SELECT wg FROM WorldGenerator wg ORDER BY wg.updatedAt DESC")
    List<WorldGenerator> findAllOrderByUpdatedAtDesc();
}
