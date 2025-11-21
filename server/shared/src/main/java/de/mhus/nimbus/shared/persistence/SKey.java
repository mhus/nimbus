package de.mhus.nimbus.shared.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persistierte Schluesseldefinition. Minimal gehalten fuer das Shared-Modul.
 */
@Entity
@Table(name = "s_key")
public class SKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Frei waehlbarer Kontexttyp (z. B. UNIVERSE/REGION/WORLD -> KeyType.name())
    @Column(nullable = false, length = 64)
    private String type;

    // Art des Schluessels: "public", "private", "symmetric"
    @Column(nullable = false, length = 16)
    private String kind;

    // Algorithmus (z. B. RSA, EC, AES, HmacSHA256)
    @Column(nullable = false, length = 64)
    private String algorithm;

    // Name/Bezeichner (hier wird KeyId.uuid() gemappt)
    @Column(nullable = false, length = 128)
    private String name;

    // Der Schluesselinhalt als Base64-kodierte Bytes
    @Lob
    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
