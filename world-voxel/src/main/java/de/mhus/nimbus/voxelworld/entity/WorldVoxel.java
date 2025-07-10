package de.mhus.nimbus.voxelworld.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.voxel.Voxel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * JPA Entity for storing voxels in the world with JSON serialization.
 * This entity provides persistence for voxel data while maintaining
 * flexibility for future enhancements through JSON storage.
 */
@Entity
@Table(name = "world_voxels",
       indexes = {
           @Index(name = "idx_world_voxel_position", columnList = "worldId, x, y, z"),
           @Index(name = "idx_world_voxel_world", columnList = "worldId"),
           @Index(name = "idx_world_voxel_chunk", columnList = "worldId, chunkX, chunkY, chunkZ"),
           @Index(name = "idx_world_voxel_modified", columnList = "lastModified")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_world_voxel_position", columnNames = {"worldId", "x", "y", "z"})
       })
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"voxelData"})
@Slf4j
public class WorldVoxel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * World identifier - references the world this voxel belongs to
     */
    @Column(nullable = false, length = 255)
    @EqualsAndHashCode.Include
    private String worldId;

    /**
     * World coordinates of the voxel
     */
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private int x;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private int y;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private int z;

    /**
     * Chunk coordinates for efficient querying
     */
    @Column(nullable = false)
    private int chunkX;

    @Column(nullable = false)
    private int chunkY;

    @Column(nullable = false)
    private int chunkZ;

    /**
     * Voxel data stored as JSON for flexibility and future compatibility
     */
    @Lob
    @Column(name = "voxel_data", nullable = false, columnDefinition = "TEXT")
    private String voxelData;

    /**
     * Timestamps for tracking changes
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastModified;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;

    /**
     * Transient field for the actual voxel object
     */
    @Transient
    @JsonIgnore
    private Voxel voxel;

    /**
     * Static ObjectMapper for JSON operations
     */
    @Transient
    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor with voxel and world ID
     *
     * @param worldId The world identifier
     * @param voxel   The voxel to store
     */
    public WorldVoxel(String worldId, Voxel voxel) {
        this.worldId = worldId;
        setVoxel(voxel);
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Sets the voxel and updates coordinates and JSON data
     *
     * @param voxel The voxel to set
     */
    public void setVoxel(Voxel voxel) {
        this.voxel = voxel;
        if (voxel != null) {
            // Update coordinates
            this.x = voxel.getX();
            this.y = voxel.getY();
            this.z = voxel.getZ();

            // Calculate chunk coordinates
            this.chunkX = Math.floorDiv(x, 16);
            this.chunkY = Math.floorDiv(y, 16);
            this.chunkZ = Math.floorDiv(z, 16);

            // Serialize to JSON
            try {
                this.voxelData = objectMapper.writeValueAsString(voxel);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize voxel to JSON for position ({}, {}, {}) in world {}",
                         x, y, z, worldId, e);
                throw new RuntimeException("Failed to serialize voxel data", e);
            }

            this.lastModified = LocalDateTime.now();
        }
    }

    /**
     * Gets the voxel object, deserializing from JSON if necessary
     *
     * @return The voxel object
     */
    public Voxel getVoxel() {
        if (voxel == null && voxelData != null) {
            try {
                voxel = objectMapper.readValue(voxelData, Voxel.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize voxel from JSON for position ({}, {}, {}) in world {}",
                         x, y, z, worldId, e);
                throw new RuntimeException("Failed to deserialize voxel data", e);
            }
        }
        return voxel;
    }

    /**
     * Updates the voxel data and marks as modified
     *
     * @param voxel The updated voxel
     */
    public void updateVoxel(Voxel voxel) {
        setVoxel(voxel);
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Checks if this voxel is in the specified chunk
     *
     * @param chunkX Chunk X coordinate
     * @param chunkY Chunk Y coordinate
     * @param chunkZ Chunk Z coordinate
     * @return true if the voxel is in the specified chunk
     */
    public boolean isInChunk(int chunkX, int chunkY, int chunkZ) {
        return this.chunkX == chunkX && this.chunkY == chunkY && this.chunkZ == chunkZ;
    }

    /**
     * Gets a string representation of the position
     *
     * @return Position as "x,y,z"
     */
    public String getPositionString() {
        return x + "," + y + "," + z;
    }

    /**
     * Gets a string representation of the chunk coordinates
     *
     * @return Chunk coordinates as "chunkX,chunkY,chunkZ"
     */
    public String getChunkString() {
        return chunkX + "," + chunkY + "," + chunkZ;
    }

    /**
     * Pre-persist callback to set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        lastModified = now;
    }

    /**
     * Pre-update callback to update modification timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    /**
     * Post-load callback to ensure voxel coordinates are in sync
     */
    @PostLoad
    protected void onLoad() {
        // Ensure transient voxel is cleared so it gets deserialized on demand
        this.voxel = null;
    }

    /**
     * Creates a copy of this WorldVoxel with a new voxel
     *
     * @param newVoxel The new voxel to copy with
     * @return A new WorldVoxel instance
     */
    public WorldVoxel copyWith(Voxel newVoxel) {
        WorldVoxel copy = new WorldVoxel(this.worldId, newVoxel);
        copy.setCreatedAt(this.createdAt);
        return copy;
    }

    /**
     * Checks if this voxel represents air (empty space)
     *
     * @return true if the voxel is air
     */
    public boolean isAir() {
        Voxel v = getVoxel();
        return v == null || v.isAir();
    }

    /**
     * Checks if this voxel is solid
     *
     * @return true if the voxel is solid
     */
    public boolean isSolid() {
        Voxel v = getVoxel();
        return v != null && v.isSolid();
    }
}
