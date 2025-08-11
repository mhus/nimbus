package de.mhus.nimbus.world.terrain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "assets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"world", "name"}),
       indexes = {
           @Index(name = "idx_assets_world", columnList = "world"),
           @Index(name = "idx_assets_world_type", columnList = "world, type")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String world;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Lob
    private byte[] data; // Uncompressed data

    @Lob
    private byte[] compressed; // Compressed data

    @Lob
    @Column(columnDefinition = "TEXT")
    private String properties; // JSON string of properties

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "compressed_at")
    private Instant compressedAt;
}
