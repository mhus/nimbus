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
@Table(name = "terrain_sprites",
       indexes = {
           @Index(name = "idx_sprites_world_level_enabled_cluster0", columnList = "world, level, enabled, cluster_x0, cluster_y0"),
           @Index(name = "idx_sprites_world_level_enabled_cluster1", columnList = "world, level, enabled, cluster_x1, cluster_y1"),
           @Index(name = "idx_sprites_world_level_enabled_cluster2", columnList = "world, level, enabled, cluster_x2, cluster_y2"),
           @Index(name = "idx_sprites_world_level_enabled_cluster3", columnList = "world, level, enabled, cluster_x3, cluster_y3")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpriteEntity {

    @Id
    private String id; // "S" + UUID

    @Column(nullable = false)
    private String world;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "cluster_x0", nullable = false)
    private Integer clusterX0; // main cluster

    @Column(name = "cluster_y0", nullable = false)
    private Integer clusterY0; // main cluster

    @Column(name = "cluster_x1")
    private Integer clusterX1; // additional cluster (right)

    @Column(name = "cluster_y1")
    private Integer clusterY1; // additional cluster (right)

    @Column(name = "cluster_x2")
    private Integer clusterX2; // additional cluster (down)

    @Column(name = "cluster_y2")
    private Integer clusterY2; // additional cluster (down)

    @Column(name = "cluster_x3")
    private Integer clusterX3; // additional cluster (down-right)

    @Column(name = "cluster_y3")
    private Integer clusterY3; // additional cluster (down-right)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String data; // JSON string of sprite data

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
