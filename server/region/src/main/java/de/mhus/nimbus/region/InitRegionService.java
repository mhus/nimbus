package de.mhus.nimbus.region;

import de.mhus.nimbus.region.registry.RRegion;
import de.mhus.nimbus.region.registry.RRegionService;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitRegionService {

    private final SKeyRepository keyRepository;
    private final KeyService keyService;
    private final RRegionService regionService;

    @PostConstruct
    public void init() {
        checkRegionJwtTokens();
    }

    private void checkRegionJwtTokens() {
        for (var regionId : regionService.listAllIds()) {
            regionService.getById(regionId).ifPresent(
                    region -> checkRegionJwtToken(region)
            );
        }
    }

    private void checkRegionJwtToken(RRegion region) {
        var intent = KeyIntent.of(region.getName(), KeyIntent.MAIN_JWT_TOKEN );
        if (keyService.getLatestPrivateKey(KeyType.REGION, intent).isEmpty()) {
            keyService.createSystemAuthKey(KeyType.REGION, intent);
        }
    }
}
