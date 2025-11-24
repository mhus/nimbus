package de.mhus.nimbus.shared.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SAssetRepository extends MongoRepository<SAsset, String> {

    List<SAsset> findByRegionId(String regionId);
    List<SAsset> findByWorldId(String worldId);
    List<SAsset> findByRegionIdAndWorldId(String regionId, String worldId);

    Optional<SAsset> findByRegionIdAndPath(String regionId, String path);
    Optional<SAsset> findByRegionIdAndWorldIdAndPath(String regionId, String worldId, String path);

    void deleteByRegionIdAndWorldIdAndPath(String regionId, String worldId, String path);
    void deleteByRegionIdAndPath(String regionId, String path);
}

