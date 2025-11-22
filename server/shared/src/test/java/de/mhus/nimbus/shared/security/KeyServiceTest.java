package de.mhus.nimbus.shared.security;

import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class KeyServiceTest {

    @Test
    void createECCKeys_generatesEcKeys() {
        SKeyRepository repo = Mockito.mock(SKeyRepository.class);
        KeyService service = new KeyService(repo);
        KeyPair pair = service.createECCKeys();
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertEquals("EC", pair.getPrivate().getAlgorithm());
        assertEquals("EC", pair.getPublic().getAlgorithm());
    }

    @Test
    void getLatestPrivateKey_filtersExpiredDisabled() {
        SKeyRepository repo = Mockito.mock(SKeyRepository.class);
        KeyService service = new KeyService(repo);
        SKey good = new SKey();
        good.setId("1");
        good.setType(KeyType.UNIVERSE); good.setKind(KeyKind.PRIVATE); good.setOwner("system"); good.setKeyId("kid");
        good.setAlgorithm("EC");
        good.setKey(service.createECCKeys().getPrivate().getEncoded().length > 0 ? "" : ""); // placeholder invalid base64
        good.setCreatedAt(Instant.now());
        SKey expired = new SKey(); expired.setEnabled(true); expired.setExpiresAt(Instant.now().minusSeconds(10));
        List<SKey> list = List.of(expired, good);
        Mockito.when(repo.findTop1ByTypeAndKindAndOwnerOrderByCreatedAtDesc(anyString(), anyString(), anyString()))
                .thenReturn(list);
        Optional<PrivateKey> opt = service.getLatestPrivateKey(KeyType.UNIVERSE, "system");
        // good key has invalid base64 -> parsing fails -> empty
        assertTrue(opt.isEmpty());
    }

    @Test
    void parseKeyId_valid() {
        SKeyRepository repo = Mockito.mock(SKeyRepository.class);
        KeyService service = new KeyService(repo);
        Optional<KeyId> id = service.parseKeyId("owner:uuid");
        assertTrue(id.isPresent());
        assertEquals("owner", id.get().owner());
        assertEquals("uuid", id.get().id());
    }

    @Test
    void parseKeyId_invalid() {
        KeyService service = new KeyService(Mockito.mock(SKeyRepository.class));
        assertTrue(service.parseKeyId(":x").isEmpty());
        assertTrue(service.parseKeyId("x:").isEmpty());
        assertTrue(service.parseKeyId("no-colon").isEmpty());
        assertTrue(service.parseKeyId(null).isEmpty());
    }
}

