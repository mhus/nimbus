package de.mhus.nimbus.shared.persistence;

import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.security.PrivateKeyProvider;
import de.mhus.nimbus.shared.security.PublicKeyProvider;
import de.mhus.nimbus.shared.security.SymmetricKeyProvider;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Implementierung der Provider-Interfaces, die Schluessel aus der SKey-Entity laedt.
 */
@Service
public class SKeyService implements PublicKeyProvider, PrivateKeyProvider, SymmetricKeyProvider {

    private final SKeyRepository repository;

    public SKeyService(SKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PublicKey> loadPublicKey(KeyType type, KeyId id) {
        return repository
                .findByTypeAndKindAndOwnerAndName(type.name(), "public", id.owner(), id.id())
                .flatMap(this::toPublicKey);
    }

    @Override
    public Optional<PrivateKey> loadPrivateKey(KeyType type, KeyId id) {
        return repository
                .findByTypeAndKindAndOwnerAndName(type.name(), "private", id.owner(), id.id())
                .flatMap(this::toPrivateKey);
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

    @Override
    public Optional<SecretKey> loadSymmetricKey(KeyType type, KeyId id) {
        return repository
                .findByTypeAndKindAndOwnerAndName(type.name(), "symmetric", id.owner(), id.id())
                .flatMap(this::toSecretKey);
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
}
