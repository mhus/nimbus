package de.mhus.nimbus.identity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * IdentityCharacter Entity - repräsentiert einen Spielercharakter im Nimbus System
 * Ein User kann mehrere IdentityCharacter besitzen
 */
@Entity
@Table(name = "player_characters")
public class IdentityCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @NotBlank
    @Column(name = "character_class", nullable = false)
    private String characterClass;

    @Column(name = "level")
    private Integer level = 1;

    @Column(name = "experience_points")
    private Long experiencePoints = 0L;

    @Column(name = "health_points")
    private Integer healthPoints = 100;

    @Column(name = "max_health_points")
    private Integer maxHealthPoints = 100;

    @Column(name = "mana_points")
    private Integer manaPoints = 100;

    @Column(name = "max_mana_points")
    private Integer maxManaPoints = 100;

    @Column(name = "current_world_id")
    private String currentWorldId;

    @Column(name = "current_planet")
    private String currentPlanet;

    @Column(name = "position_x")
    private Double positionX = 0.0;

    @Column(name = "position_y")
    private Double positionY = 0.0;

    @Column(name = "position_z")
    private Double positionZ = 0.0;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Many-to-One Beziehung zu User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Default constructor
    public IdentityCharacter() {}

    // Constructor
    public IdentityCharacter(String name, String characterClass, User user) {
        this.name = name;
        this.characterClass = characterClass;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getExperiencePoints() {
        return experiencePoints;
    }

    public void setExperiencePoints(Long experiencePoints) {
        this.experiencePoints = experiencePoints;
    }

    public Integer getHealthPoints() {
        return healthPoints;
    }

    public void setHealthPoints(Integer healthPoints) {
        this.healthPoints = healthPoints;
    }

    public Integer getMaxHealthPoints() {
        return maxHealthPoints;
    }

    public void setMaxHealthPoints(Integer maxHealthPoints) {
        this.maxHealthPoints = maxHealthPoints;
    }

    public Integer getManaPoints() {
        return manaPoints;
    }

    public void setManaPoints(Integer manaPoints) {
        this.manaPoints = manaPoints;
    }

    public Integer getMaxManaPoints() {
        return maxManaPoints;
    }

    public void setMaxManaPoints(Integer maxManaPoints) {
        this.maxManaPoints = maxManaPoints;
    }

    public String getCurrentWorldId() {
        return currentWorldId;
    }

    public void setCurrentWorldId(String currentWorldId) {
        this.currentWorldId = currentWorldId;
    }

    public String getCurrentPlanet() {
        return currentPlanet;
    }

    public void setCurrentPlanet(String currentPlanet) {
        this.currentPlanet = currentPlanet;
    }

    public Double getPositionX() {
        return positionX;
    }

    public void setPositionX(Double positionX) {
        this.positionX = positionX;
    }

    public Double getPositionY() {
        return positionY;
    }

    public void setPositionY(Double positionY) {
        this.positionY = positionY;
    }

    public Double getPositionZ() {
        return positionZ;
    }

    public void setPositionZ(Double positionZ) {
        this.positionZ = positionZ;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "IdentityCharacter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", characterClass='" + characterClass + '\'' +
                ", level=" + level +
                ", currentPlanet='" + currentPlanet + '\'' +
                ", active=" + active +
                '}';
    }
}
