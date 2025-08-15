package de.mhus.nimbus.world.terrain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "terrain_materials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Boolean blocking;

    private Float friction;

    private String color;

    private String texture;

    @Column(name = "sound_walk")
    private String soundWalk;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String properties; // JSON string
}
