package de.mhus.nimbus.world.terrain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "map_clusters",
       uniqueConstraints = @UniqueConstraint(columnNames = {"world", "level", "cluster_x", "cluster_y"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapCluster {

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
    private String data;

    @Lob
    private byte[] compressed;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "compressed_at")
    private LocalDateTime compressedAt;
}
