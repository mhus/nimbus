package de.mhus.nimbus.shared.security;

import de.mhus.nimbus.shared.persistence.SKey;
import de.mhus.nimbus.shared.persistence.SKeyRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Implementierung der Provider-Interfaces, die Schlüssel aus der SKey-Entity lädt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeyService {

    private static final String KIND_PRIVATE = KeyKind.PRIVATE.name();
    private static final String KIND_PUBLIC = KeyKind.PUBLIC.name();
    private static final String KIND_SECRET = KeyKind.SECRET.name();

    private final SKeyRepository repository;

    public Optional<PublicKey> getPublicKey(KeyType type, KeyId id) {
        return repository
                .findByTypeAndKindAndOwnerAndKeyId(type.name(), KIND_PUBLIC, id.owner(), id.id())
                .flatMap(this::toPublicKey);
    }

    public List<PublicKey> getPublicKeysForOwner(KeyType type, String owner) {
        return repository.findAllByTypeAndKindAndOwnerOrderByCreatedAtDesc(type.name(), KIND_PUBLIC, owner)
                .stream()
                .filter(sKey -> sKey.isEnabled() && !sKey.isExpired())
                .map(key -> toPublicKey(key).orElse(null))
                .filter(key -> key != null)
                .toList();
    }

    public Optional<PrivateKey> getPrivateKey(KeyType type, KeyId id) {
        return repository
                .findByTypeAndKindAndOwnerAndKeyId(type.name(), KIND_PRIVATE, id.owner(), id.id())
                .flatMap(this::toPrivateKey);
    }

    public List<PrivateKey> getPrivateKeysForOwner(KeyType type, String owner) {
        return repository.findAllByTypeAndKindAndOwnerOrderByCreatedAtDesc(type.name(), KIND_PRIVATE, owner)
                .stream()
                .filter(sKey -> sKey.isEnabled() && !sKey.isExpired())
                .map(key -> toPrivateKey(key).orElse(null))
                .filter(key -> key != null)
                .toList();
    }

    private Optional<PrivateKey> toPrivateKey(SKey sKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(sKey.getKey());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance(sKey.getAlgorithm());
            return Optional.of(kf.generatePrivate(spec));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<SecretKey> getSecretKey(KeyType type, KeyId id) {
        return repository
                .findByTypeAndKindAndOwnerAndKeyId(type.name(), KIND_SECRET, id.owner(), id.id())
                .flatMap(this::toSecretKey);
    }

    public List<SecretKey> getSecretKeysForOwner(KeyType type, String owner) {
        // Bugfix: vorher KIND_PRIVATE, korrekt ist KIND_SECRET ("symmetric")
        return repository.findAllByTypeAndKindAndOwnerOrderByCreatedAtDesc(type.name(), KIND_SECRET, owner)
                .stream()
                .filter(sKey -> sKey.isEnabled() && !sKey.isExpired())
                .map(key -> toSecretKey(key).orElse(null))
                .filter(key -> key != null)
                .toList();
    }

    private Optional<PublicKey> toPublicKey(SKey entity) {
        try {
            byte[] encoded = Base64.getDecoder().decode(entity.getKey());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance(entity.getAlgorithm());
            return Optional.of(kf.generatePublic(spec));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<SecretKey> toSecretKey(SKey entity) {
        try {
            byte[] encoded = Base64.getDecoder().decode(entity.getKey());
            return Optional.of(new SecretKeySpec(encoded, entity.getAlgorithm()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<PrivateKey> getLatestPrivateKey(KeyType keyType, KeyIntent intent) {
        return repository.findTop1ByTypeAndKindAndOwnerAndIntentOrderByCreatedAtDesc(keyType.name(), KIND_PRIVATE, intent.owner(), intent.intent())
                .stream()
                .filter(SKey::isEnabled)
                .filter(k -> !k.isExpired())
                .findFirst()
                .flatMap(this::toPrivateKey);
    }

    public Optional<SecretKey> getLatestSecretKey(KeyType keyType, String owner) {
        return repository.findTop1ByTypeAndKindAndOwnerOrderByCreatedAtDesc(keyType.name(), KIND_SECRET, owner)
                .stream()
                .filter(SKey::isEnabled)
                .filter(k -> !k.isExpired())
                .findFirst()
                .flatMap(this::toSecretKey);
    }

    public Optional<SecretKey> getSecretKey(KeyType keyType, @NonNull String keyId) {
        return getSecretKey(keyType, parseKeyId(keyId).get());
    }

    public Optional<PublicKey> getPublicKey(KeyType keyType, @NonNull String keyId) {
        return getPublicKey(keyType, parseKeyId(keyId).get());
    }

    public Optional<PrivateKey> getPrivateKey(KeyType keyType, @NonNull String keyId) {
        return getPrivateKey(keyType, parseKeyId(keyId).get());
    }

    /**
     * Parses a string in the form "owner:id" into a KeyId.
     * Returns Optional.empty() if the input is null, blank, or malformed.
     */
    public Optional<KeyId> parseKeyId(String keyId) {
        if (keyId == null) return Optional.empty();
        String trimmed = keyId.trim();
        if (trimmed.isEmpty()) return Optional.empty();
        String[] parts = trimmed.split(":", 3);
        if (parts.length != 3) return Optional.empty();
        String owner = parts[0].trim();
        String intent = parts[1].trim();
        String id = parts[2].trim();
        return Optional.of(KeyId.of(owner, intent, id));
    }

    public KeyPair createECCKeys() {
        // Erzeugt ein EC Schlüsselpaar mit Standard-Kurve secp256r1 (NIST P-256)
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("EC KeyPair Generierung fehlgeschlagen", e);
        }
    }

    public KeyId generateKeyId(KeyIntent intent) {
        return new KeyId(intent, java.util.UUID.randomUUID().toString());
    }

    public void storeKeyPair(KeyType keyType, KeyId keyId, KeyPair keyPair) {
        repository.save(SKey.ofPrivateKey(keyType, keyId, keyPair.getPrivate()));
        repository.save(SKey.ofPublicKey(keyType, keyId, keyPair.getPublic()));
    }

    public void deleteAllForIntent(KeyIntent intent) {
        repository.deleteAllByTypeAndKindAndOwnerAndIntent(KeyType.UNIVERSE.name(), KeyKind.PUBLIC.name(), intent.owner(), intent.intent());
        repository.deleteAllByTypeAndKindAndOwnerAndIntent(KeyType.UNIVERSE.name(), KeyKind.PRIVATE.name(), intent.owner(), intent.intent());
        repository.deleteAllByTypeAndKindAndOwnerAndIntent(KeyType.UNIVERSE.name(), KeyKind.SECRET.name(), intent.owner(), intent.intent());
    }

    public void createSystemAuthKey(KeyType type, KeyIntent intent) {
        try {
            var keyPair = createECCKeys();
            deleteAllForIntent(intent);

            var jwtKeyId = generateKeyId(intent);
            storeKeyPair(type, jwtKeyId, keyPair);

        } catch (Exception e) {
            log.error("ECC KeyPair-Erstellung fehlgeschlagen: {}", e.getMessage(), e);
        }
    }
}
