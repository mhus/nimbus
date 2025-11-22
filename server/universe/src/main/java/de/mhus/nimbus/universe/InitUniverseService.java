package de.mhus.nimbus.universe;

import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.user.UniverseRoles;
import de.mhus.nimbus.universe.security.USecurityProperties;
import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.universe.user.UUserRepository;
import de.mhus.nimbus.universe.user.UUserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logging
import org.springframework.stereotype.Service;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitUniverseService {

    private final SKeyRepository keyRepository;
    private final KeyService keyService;
    private UUserRepository userRepository;
    private UUserService userService;

    @PostConstruct
    public void init() {
        checkAuthToken();
        checkRefreshToken();
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
        userRepository.save(admin);
        userService.setPassword(admin.getId(), password);
        log.info("Admin user created: {} with Password {}", admin.getId(), password);
    }

    private void checkRefreshToken() {
        var jwtKeyId = keyService.parseKeyId(USecurityProperties.JWT_REFRESH_TOKEN_KEY_ID).get();
        if (!keyRepository.findByTypeAndKindAndKeyId(KeyType.UNIVERSE.name(), jwtKeyId.owner(), jwtKeyId.id() ).isPresent()) {
            createSystemAuthKey(jwtKeyId);
        }
    }

    private void checkAuthToken() {
        var jwtKeyId = keyService.parseKeyId(USecurityProperties.JWT_AUTH_TOKEN_KEY_ID).get();
        if (!keyRepository.findByTypeAndKindAndKeyId(KeyType.UNIVERSE.name(), jwtKeyId.owner(), jwtKeyId.id() ).isPresent()) {
            createSystemAuthKey(jwtKeyId);
        }
    }

    private void createSystemAuthKey(KeyId keyId) {
        try {
            var keyPair = keyService.createECCKeys();
            keyRepository.deleteByTypeAndKindAndKeyId(KeyType.UNIVERSE.name(), "public", keyId.id());
            keyRepository.deleteByTypeAndKindAndKeyId(KeyType.UNIVERSE.name(), "private", keyId.id());

            keyRepository.save(SKey.ofPrivateKey(KeyType.UNIVERSE, keyId.owner(), keyId.id(), keyPair.getPrivate()));
            keyRepository.save(SKey.ofPublicKey(KeyType.UNIVERSE, keyId.owner(), keyId.id(), keyPair.getPublic()));
        } catch (Exception e) {
            log.error("ECC KeyPair-Erstellung fehlgeschlagen: {}", e.getMessage(), e);
        }
    }

}
