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
@Table(name = "sprites",
       indexes = {
           @Index(columnList = "world, level, enabled, cluster_x0, cluster_y0"),
           @Index(columnList = "world, level, enabled, cluster_x1, cluster_y1"),
           @Index(columnList = "world, level, enabled, cluster_x2, cluster_y2"),
           @Index(columnList = "world, level, enabled, cluster_x3, cluster_y3")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprite {

    @Id
    private String id; // "S" + UUID for static sprites

    @Column(nullable = false)
    private String world;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "cluster_x0", nullable = false)
    private Integer clusterX0;

    @Column(name = "cluster_y0", nullable = false)
    private Integer clusterY0;

    @Column(name = "cluster_x1")
    private Integer clusterX1;

    @Column(name = "cluster_y1")
    private Integer clusterY1;

    @Column(name = "cluster_x2")
    private Integer clusterX2;

    @Column(name = "cluster_y2")
    private Integer clusterY2;

    @Column(name = "cluster_x3")
    private Integer clusterX3;

    @Column(name = "cluster_y3")
    private Integer clusterY3;

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
