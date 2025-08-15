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
@Table(name = "terrain_maps",
       uniqueConstraints = @UniqueConstraint(columnNames = {"world", "level", "cluster_x", "cluster_y"}),
       indexes = {
           @Index(name = "idx_maps_world_level", columnList = "world, level"),
           @Index(name = "idx_maps_cluster", columnList = "world, level, cluster_x, cluster_y")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String world;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "cluster_x", nullable = false)
    private Integer clusterX;

    @Column(name = "cluster_y", nullable = false)
    private Integer clusterY;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String data; // JSON string of cluster fields

    @Lob
    private byte[] compressed; // Compressed data

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "compressed_at")
    private Instant compressedAt;
}
