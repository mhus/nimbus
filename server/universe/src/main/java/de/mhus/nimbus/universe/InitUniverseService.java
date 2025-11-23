package de.mhus.nimbus.universe;

import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.user.UniverseRoles;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.universe.user.UUserRepository;
import de.mhus.nimbus.universe.user.UUserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logging
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitUniverseService {

    private final SKeyRepository keyRepository;
    private final KeyService keyService;
    private final UUserRepository userRepository;
    private final UUserService userService;

    @PostConstruct
    public void init() {
        checkUniverseJwtToken();
        checkAdminUser();
    }

    private void checkAdminUser() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            createAdminUser();
        }
    }

    private void createAdminUser() {
        UUser admin = new UUser("admin","");
        var password = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        admin.setRoles(Set.of(UniverseRoles.ADMIN));
        admin.setEnabled(true);
        userRepository.save(admin);
        userService.setPassword(admin.getId(), password);
        log.info("Admin user created: {} with Password {}", admin.getId(), password);
    }

    private void checkUniverseJwtToken() {
        if (keyService.getLatestPrivateKey(KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT).isEmpty()) {
            keyService.createSystemAuthKey(KeyType.UNIVERSE, UniverseProperties.MAIN_JWT_TOKEN_INTENT);
        }
    }

}
