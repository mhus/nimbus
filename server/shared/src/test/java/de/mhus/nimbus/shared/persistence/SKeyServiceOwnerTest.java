package de.mhus.nimbus.shared.persistence;

import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.KeyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SKeyServiceOwnerTest {

    @Mock
    private SKeyRepository repository;

    private SKeyService sKeyService;

    private PublicKey publicKeyOwnerA;
    private PrivateKey privateKeyOwnerA;
    private SecretKey secretKeyOwnerA;

    @BeforeEach
    void setUp() throws Exception {
        sKeyService = new SKeyService(repository);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();
        publicKeyOwnerA = kp.getPublic();
        privateKeyOwnerA = kp.getPrivate();
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        secretKeyOwnerA = kg.generateKey();
    }

    @Test
    void loadPublicKey_ownerMatch_shouldReturnKey() {
        SKey entity = SKey.ofPublicKey(KeyType.UNIVERSE.name(), "ownerA", "myKey", publicKeyOwnerA);
        when(repository.findByTypeAndKindAndOwnerAndName(KeyType.UNIVERSE.name(), "public", "ownerA", "myKey"))
                .thenReturn(Optional.of(entity));

        Optional<PublicKey> result = sKeyService.loadPublicKey(KeyType.UNIVERSE, KeyId.of("ownerA", "myKey"));
        assertThat(result).isPresent();
        assertThat(result.get().getAlgorithm()).isEqualTo(publicKeyOwnerA.getAlgorithm());
    }

    @Test
    void loadPublicKey_ownerMismatch_shouldReturnEmpty() {
        // Kein Stub – anderer Owner soll leer liefern
        Optional<PublicKey> result = sKeyService.loadPublicKey(KeyType.UNIVERSE, KeyId.of("ownerB", "myKey"));
        assertThat(result).isEmpty();
    }

    @Test
    void loadPrivateKey_ownerMatch_shouldReturnKey() {
        SKey entity = SKey.ofPrivateKey(KeyType.UNIVERSE.name(), "ownerA", "privKey", privateKeyOwnerA);
        when(repository.findByTypeAndKindAndOwnerAndName(KeyType.UNIVERSE.name(), "private", "ownerA", "privKey"))
                .thenReturn(Optional.of(entity));
        Optional<PrivateKey> result = sKeyService.loadPrivateKey(KeyType.UNIVERSE, KeyId.of("ownerA", "privKey"));
        assertThat(result).isPresent();
    }

    @Test
    void loadPrivateKey_ownerMismatch_shouldReturnEmpty() {
        // Kein Stub – anderer Owner soll leer liefern
        Optional<PrivateKey> result = sKeyService.loadPrivateKey(KeyType.UNIVERSE, KeyId.of("ownerB", "privKey"));
        assertThat(result).isEmpty();
    }

    @Test
    void loadSymmetricKey_ownerMatch_shouldReturnKey() {
        SKey entity = SKey.ofSecretKey(KeyType.UNIVERSE.name(), "ownerA", "syncKey", secretKeyOwnerA);
        when(repository.findByTypeAndKindAndOwnerAndName(KeyType.UNIVERSE.name(), "symmetric", "ownerA", "syncKey"))
                .thenReturn(Optional.of(entity));
        Optional<SecretKey> result = sKeyService.loadSymmetricKey(KeyType.UNIVERSE, KeyId.of("ownerA", "syncKey"));
        assertThat(result).isPresent();
        assertThat(result.get().getAlgorithm()).isEqualTo(secretKeyOwnerA.getAlgorithm());
    }

    @Test
    void loadSymmetricKey_ownerMismatch_shouldReturnEmpty() {
        // Kein Stub – anderer Owner soll leer liefern
        Optional<SecretKey> result = sKeyService.loadSymmetricKey(KeyType.UNIVERSE, KeyId.of("ownerB", "syncKey"));
        assertThat(result).isEmpty();
    }
}
