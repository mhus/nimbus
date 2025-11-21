package de.mhus.nimbus.universe;

import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.universe.security.JwtProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logging
import org.springframework.stereotype.Service;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitUniverseBean {

    private final SKeyRepository keyRepository;
    private final JwtProperties jwtProperties;
    private final KeyService keyService;

    @PostConstruct
    public void init() {
        var jwtKeyId = keyService.parseKeyId(jwtProperties.getKeyId()).get();
        if (!keyRepository.findByTypeAndKindAndName(KeyType.UNIVERSE.name(), jwtKeyId.owner(), jwtKeyId.id() ).isPresent()) {
            createSystemAuthKey(jwtKeyId);
        }

    }

    private void createSystemAuthKey(KeyId keyId) {
        try {
            var keyPair = keyService.createECCKeys();
            keyRepository.save(SKey.ofPrivateKey(KeyType.UNIVERSE.name(), keyId.owner(), keyId.id(), keyPair.getPrivate()));
            keyRepository.save(SKey.ofPublicKey(KeyType.UNIVERSE.name(), keyId.owner(), keyId.id(), keyPair.getPublic()));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            log.error("ECC KeyPair-Erstellung fehlgeschlagen: {}", e.getMessage(), e);
        }
    }

}
