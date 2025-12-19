package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for WAnythingEntity.
 * Provides query methods for flexible data retrieval by region, world, collection, and name.
 */
@Repository
public interface WAnythingRepository extends MongoRepository<WAnythingEntity, String> {

    Optional<WAnythingEntity> findByCollectionAndName(String collection, String name);

    Optional<WAnythingEntity> findByWorldIdAndCollectionAndName(String worldId, String collection, String name);

    Optional<WAnythingEntity> findByRegionIdAndCollectionAndName(String regionId, String collection, String name);

    Optional<WAnythingEntity> findByRegionIdAndWorldIdAndCollectionAndName(String regionId, String worldId, String collection, String name);

    List<WAnythingEntity> findByCollection(String collection);

    List<WAnythingEntity> findByWorldIdAndCollection(String worldId, String collection);

    List<WAnythingEntity> findByRegionIdAndCollection(String regionId, String collection);

    List<WAnythingEntity> findByRegionIdAndWorldIdAndCollection(String regionId, String worldId, String collection);

    List<WAnythingEntity> findByCollectionAndEnabled(String collection, boolean enabled);

    List<WAnythingEntity> findByWorldIdAndCollectionAndEnabled(String worldId, String collection, boolean enabled);

    List<WAnythingEntity> findByRegionIdAndCollectionAndEnabled(String regionId, String collection, boolean enabled);

    List<WAnythingEntity> findByRegionIdAndWorldIdAndCollectionAndEnabled(String regionId, String worldId, String collection, boolean enabled);

    List<WAnythingEntity> findByCollectionAndType(String collection, String type);

    List<WAnythingEntity> findByWorldIdAndCollectionAndType(String worldId, String collection, String type);

    List<WAnythingEntity> findByRegionIdAndCollectionAndType(String regionId, String collection, String type);

    List<WAnythingEntity> findByRegionIdAndWorldIdAndCollectionAndType(String regionId, String worldId, String collection, String type);

    boolean existsByCollectionAndName(String collection, String name);

    boolean existsByWorldIdAndCollectionAndName(String worldId, String collection, String name);

    boolean existsByRegionIdAndCollectionAndName(String regionId, String collection, String name);

    boolean existsByRegionIdAndWorldIdAndCollectionAndName(String regionId, String worldId, String collection, String name);

    void deleteByCollectionAndName(String collection, String name);

    void deleteByWorldIdAndCollectionAndName(String worldId, String collection, String name);

    void deleteByRegionIdAndCollectionAndName(String regionId, String collection, String name);

    void deleteByRegionIdAndWorldIdAndCollectionAndName(String regionId, String worldId, String collection, String name);
}
