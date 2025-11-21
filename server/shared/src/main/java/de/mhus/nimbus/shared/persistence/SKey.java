package de.mhus.nimbus.shared.persistence;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

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

    // Name/Bezeichner (hier wird KeyId.uuid() gemappt)
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
}
