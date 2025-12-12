package de.mhus.nimbus.region;

import de.mhus.nimbus.world.shared.region.RRegionService;
import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.utils.ConfidentialUtil;
import de.mhus.nimbus.world.shared.region.RegionProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitRegionService {

    private final KeyService keyService;
    private final RRegionService regionService;
    private final RegionProperties regionProperties;

    @PostConstruct
    public void init() {
        checkRegionServerJwtToken();
        checkRegionJwtTokens();
    }

    private void checkRegionJwtTokens() {
        for (var regionId : regionService.listAllIds()) {
            regionService.getRegionNameById(regionId).ifPresent(
                    regionName -> checkRegionJwtToken(regionName)
            );
        }
    }

    private void checkRegionJwtToken(String regionName) {
        var intent = KeyIntent.of(regionName, KeyIntent.REGION_JWT_TOKEN);
        if (keyService.getLatestPrivateKey(KeyType.REGION, intent).isEmpty()) {
            var keys = keyService.createECCKeys();
            keyService.storeKeyPair(
                    KeyType.REGION,
                    KeyId.newOf(intent),
                    keys
            );
            log.info("Created missing JWT token key for region '{}'", regionName);
        }
    }

    private void checkRegionServerJwtToken() {
        var intent = KeyIntent.of(regionProperties.getRegionServerId(), KeyIntent.REGION_SERVER_JWT_TOKEN);
        if (keyService.getLatestPrivateKey(KeyType.REGION, intent).isEmpty()) {
            var keys = keyService.createECCKeys();
            keyService.storeKeyPair(
                    KeyType.REGION,
                    KeyId.newOf(intent),
                    keys
            );
            log.info("Created missing JWT token key for region server'{}'", regionProperties.getRegionServerId());
            ConfidentialUtil.save("regionServerPublicKey.txt", keys.getPublic());
        }
    }

}
