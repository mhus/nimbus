package de.mhus.nimbus.region.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import de.mhus.nimbus.shared.user.RegionRoles;

@Repository
public interface RRegionRoleRepository extends MongoRepository<RRegionRole, String> {
    Optional<RRegionRole> findByUserIdAndRegionId(String userId, String regionId);
    List<RRegionRole> findByUserId(String userId);
    List<RRegionRole> findByRegionId(String regionId);
    List<RRegionRole> findByRegionIdAndRolesIn(String regionId, List<RegionRoles> roles);
}
