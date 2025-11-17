package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.KeyId;
import de.mhus.nimbus.shared.security.SecretKeyProvider;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;

@Component
public class StaticSecretKeyProvider implements SecretKeyProvider {

    private final JwtProperties properties;
    private final SecretKey secretKey;
    private final KeyId configuredId;

    public StaticSecretKeyProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Base64.getDecoder().decode(properties.getSecretBase64());
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        var parts = properties.getKeyId().split(":",2);
        this.configuredId = KeyId.of(parts[0], parts[1]);
    }

    @Override
    public Optional<SecretKey> loadSecretKey(KeyId id) {
        if (id.owner().equals(configuredId.owner()) && id.uuid().equals(configuredId.uuid())) {
            return Optional.of(secretKey);
        }
        return Optional.empty();
    }
}

