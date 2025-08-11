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
@Table(name = "terrain_groups",
       uniqueConstraints = @UniqueConstraint(columnNames = {"world", "id"}),
       indexes = {
           @Index(name = "idx_terrain_groups_world", columnList = "world")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerrainGroupEntity {

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
    @Column(columnDefinition = "TEXT")
    private String data; // JSON string of group properties

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
