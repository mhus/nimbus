package de.mhus.nimbus.shared.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SAssetRepository extends MongoRepository<SAsset, String> {

    List<SAsset> findByWorldId(String worldId);

    Optional<SAsset> findByWorldIdAndPath(String worldId, String path);

    void deleteByWorldIdAndPath(String worldId, String path);

}

