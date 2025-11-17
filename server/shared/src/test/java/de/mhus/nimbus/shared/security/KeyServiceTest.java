package de.mhus.nimbus.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class KeyServiceTest {

    private KeyService keyService;
    private PublicKey testPublicKey;
    private SecretKey testSecretKey;

    // Simple ObjectProvider implementation for testing
    private static class TestObjectProvider<T> implements ObjectProvider<T> {
        private final Supplier<T> supplier;

        public TestObjectProvider(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T getObject(Object... args) {
            return supplier.get();
        }

        @Override
        public T getIfAvailable() {
            return supplier.get();
        }

        @Override
        public T getIfUnique() {
            return supplier.get();
        }

        @Override
        public T getObject() {
            return supplier.get();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        testPublicKey = keyPair.getPublic();
        testSecretKey = new SecretKeySpec(new byte[32], "HmacSHA256");
    }

    // ================================================================
    // parseKeyId tests
    // ================================================================

    @Test
    void parseKeyId_validFormat_shouldReturnKeyId() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<KeyId> result = keyService.parseKeyId("owner:uuid");

        assertThat(result).isPresent();
        assertThat(result.get().owner()).isEqualTo("owner");
        assertThat(result.get().uuid()).isEqualTo("uuid");
    }

    @Test
    void parseKeyId_validFormatWithSpaces_shouldTrimAndReturnKeyId() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<KeyId> result = keyService.parseKeyId("  owner  :  uuid  ");

        assertThat(result).isPresent();
        assertThat(result.get().owner()).isEqualTo("owner");
        assertThat(result.get().uuid()).isEqualTo("uuid");
    }

    @Test
    void parseKeyId_nullInput_shouldReturnEmpty() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<KeyId> result = keyService.parseKeyId(null);

        assertThat(result).isEmpty();
    }

    @Test
    void parseKeyId_invalidFormat_shouldReturnEmpty() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        assertThat(keyService.parseKeyId("")).isEmpty();
        assertThat(keyService.parseKeyId("   ")).isEmpty();
        assertThat(keyService.parseKeyId("owneruuid")).isEmpty();
        assertThat(keyService.parseKeyId(":uuid")).isEmpty();
        assertThat(keyService.parseKeyId("owner:")).isEmpty();
        assertThat(keyService.parseKeyId(":")).isEmpty();
    }

    // ================================================================
    // findPublicKey tests
    // ================================================================

    @Test
    void findPublicKey_withValidKeyId_shouldReturnKey() {
        KeyId keyId = KeyId.of("owner", "uuid");
        PublicKeyProvider provider = id -> id.equals(keyId) ? Optional.of(testPublicKey) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(() -> List.of(provider)),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<PublicKey> result = keyService.findPublicKey(keyId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPublicKey);
    }

    @Test
    void findPublicKey_withStringKeyId_shouldReturnKey() {
        KeyId keyId = KeyId.of("owner", "uuid");
        PublicKeyProvider provider = id -> id.equals(keyId) ? Optional.of(testPublicKey) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(() -> List.of(provider)),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<PublicKey> result = keyService.findPublicKey("owner:uuid");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPublicKey);
    }

    @Test
    void findPublicKey_noProviders_shouldReturnEmpty() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<PublicKey> result = keyService.findPublicKey("owner:uuid");

        assertThat(result).isEmpty();
    }

    @Test
    void findPublicKey_providerReturnsEmpty_shouldReturnEmpty() {
        PublicKeyProvider provider = id -> Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(() -> List.of(provider)),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<PublicKey> result = keyService.findPublicKey("owner:uuid");

        assertThat(result).isEmpty();
    }

    // ================================================================
    // findSecretKey tests
    // ================================================================

    @Test
    void findSecretKey_withValidKeyId_shouldReturnKey() {
        KeyId keyId = KeyId.of("owner", "uuid");
        SecretKeyProvider provider = id -> id.equals(keyId) ? Optional.of(testSecretKey) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(() -> List.of(provider)),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<SecretKey> result = keyService.findSecretKey(keyId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSecretKey);
    }

    @Test
    void findSecretKey_withStringKeyId_shouldReturnKey() {
        KeyId keyId = KeyId.of("owner", "uuid");
        SecretKeyProvider provider = id -> id.equals(keyId) ? Optional.of(testSecretKey) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(() -> List.of(provider)),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<SecretKey> result = keyService.findSecretKey("owner:uuid");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSecretKey);
    }

    @Test
    void findSecretKey_noProviders_shouldReturnEmpty() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<SecretKey> result = keyService.findSecretKey("owner:uuid");

        assertThat(result).isEmpty();
    }

    // ================================================================
    // findSyncKey tests
    // ================================================================

    @Test
    void findSyncKey_withValidKeyId_shouldReturnKey() {
        KeyId keyId = KeyId.of("owner", "uuid");
        SyncKeyProvider provider = id -> id.equals(keyId) ? Optional.of(testSecretKey) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(() -> List.of(provider))
        );

        Optional<SecretKey> result = keyService.findSyncKey(keyId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSecretKey);
    }

    @Test
    void findSyncKey_withStringKeyId_shouldReturnKey() {
        KeyId keyId = KeyId.of("owner", "uuid");
        SyncKeyProvider provider = id -> id.equals(keyId) ? Optional.of(testSecretKey) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(() -> List.of(provider))
        );

        Optional<SecretKey> result = keyService.findSyncKey("owner:uuid");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSecretKey);
    }

    @Test
    void findSyncKey_noProviders_shouldReturnEmpty() {
        keyService = new KeyService(
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<SecretKey> result = keyService.findSyncKey("owner:uuid");

        assertThat(result).isEmpty();
    }

    // ================================================================
    // Multiple providers tests
    // ================================================================

    @Test
    void findPublicKey_multipleProviders_firstReturnsKey_shouldReturnFromFirst() throws Exception {
        KeyId keyId = KeyId.of("owner", "uuid");
        PublicKeyProvider provider1 = id -> id.equals(keyId) ? Optional.of(testPublicKey) : Optional.empty();
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        PublicKey testPublicKey2 = keyPairGenerator.generateKeyPair().getPublic();
        PublicKeyProvider provider2 = id -> id.equals(keyId) ? Optional.of(testPublicKey2) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(() -> List.of(provider1, provider2)),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<PublicKey> result = keyService.findPublicKey(keyId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPublicKey); // Should get from first provider
    }

    @Test
    void findPublicKey_multipleProviders_firstReturnsEmpty_shouldReturnFromSecond() throws Exception {
        KeyId keyId = KeyId.of("owner", "uuid");
        PublicKeyProvider provider1 = id -> Optional.empty();
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        PublicKey testPublicKey2 = keyPairGenerator.generateKeyPair().getPublic();
        PublicKeyProvider provider2 = id -> id.equals(keyId) ? Optional.of(testPublicKey2) : Optional.empty();

        keyService = new KeyService(
            new TestObjectProvider<>(() -> List.of(provider1, provider2)),
            new TestObjectProvider<>(Collections::emptyList),
            new TestObjectProvider<>(Collections::emptyList)
        );

        Optional<PublicKey> result = keyService.findPublicKey(keyId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPublicKey2); // Should get from second provider
    }
}
