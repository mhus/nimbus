package de.mhus.nimbus.shared.persistence;

import de.mhus.nimbus.shared.security.Base64Service;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Persisted key definition to be stored in MongoDB.
 */
@Document(collection = "s_keys")
public class SKey {

    @Id
    private String id;

    // Frei waehlbarer Kontexttyp (z. B. UNIVERSE/REGION/WORLD -> KeyType.name())
    private String type;

    // Art des Schluessels: "public", "private", "symmetric"
    private String kind;

    // Algorithmus (z. B. RSA, EC, AES, HmacSHA256)
    private String algorithm;

    // Name/Bezeichner (hier wird KeyId.id() gemappt)
    private String name;

    // Der Schluesselinhalt als Base64-kodierte Bytes
    private String key;

    @CreatedDate
    private Instant createdAt;

    // Getter/Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public SKey() {}

    // Convenience-Konstruktor für generischen Eintrag
    public SKey(String type, String kind, String name, String algorithm, String base64Key) {
        this.type = type;
        this.kind = kind;
        this.name = name;
        this.algorithm = algorithm;
        this.key = base64Key;
    }

    // Factory-Methoden
    public static SKey ofPublicKey(String type, String owner, String name, PublicKey key) {
        String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return new SKey(type, "public", name, key.getAlgorithm(), base64);
    }

    public static SKey ofPrivateKey(String type, String owner, String name, PrivateKey key) {
        String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return new SKey(type, "private", name, key.getAlgorithm(), base64);
    }

    public static SKey ofSecretKey(String type, String owner, String name, SecretKey key) {
        String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return new SKey(type, "symmetric", name, key.getAlgorithm(), base64);
    }
}
