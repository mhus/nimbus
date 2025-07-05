package de.mhus.nimbus.registry.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA-Entität für Planeten
 */
@Entity
@Table(name = "planets", indexes = {
    @Index(name = "idx_planet_name_env", columnList = "name, environment")
})
public class Planet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private de.mhus.nimbus.shared.avro.Environment environment;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "galaxy", length = 100)
    private String galaxy;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "system_name", length = 100)
    private String systemName;

    @Column(name = "population")
    private Long population;

    @Column(name = "climate", length = 50)
    private String climate;

    @Column(name = "terrain", length = 100)
    private String terrain;

    @Column(name = "surface_water")
    private Integer surfaceWater;

    @Column(name = "gravity")
    private String gravity;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "planet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<World> worlds = new ArrayList<>();

    // Constructors
    public Planet() {}

    public Planet(String name, de.mhus.nimbus.shared.avro.Environment environment) {
        this.name = name;
        this.environment = environment;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public de.mhus.nimbus.shared.avro.Environment getEnvironment() { return environment; }
    public void setEnvironment(de.mhus.nimbus.shared.avro.Environment environment) { this.environment = environment; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGalaxy() { return galaxy; }
    public void setGalaxy(String galaxy) { this.galaxy = galaxy; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public Long getPopulation() { return population; }
    public void setPopulation(Long population) { this.population = population; }

    public String getClimate() { return climate; }
    public void setClimate(String climate) { this.climate = climate; }

    public String getTerrain() { return terrain; }
    public void setTerrain(String terrain) { this.terrain = terrain; }

    public Integer getSurfaceWater() { return surfaceWater; }
    public void setSurfaceWater(Integer surfaceWater) { this.surfaceWater = surfaceWater; }

    public String getGravity() { return gravity; }
    public void setGravity(String gravity) { this.gravity = gravity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<World> getWorlds() { return worlds; }
    public void setWorlds(List<World> worlds) { this.worlds = worlds; }

    public void addWorld(World world) {
        worlds.add(world);
        world.setPlanet(this);
    }

    public void removeWorld(World world) {
        worlds.remove(world);
        world.setPlanet(null);
    }
}
