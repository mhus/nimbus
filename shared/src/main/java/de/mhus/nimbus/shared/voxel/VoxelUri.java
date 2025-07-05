package de.mhus.nimbus.shared.voxel;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unique identifier for voxel types
 * Similar to BlockUri in Terasology
 */
@Getter
@EqualsAndHashCode
@ToString
public class VoxelUri implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String SEPARATOR = ":";

    private final String moduleId;
    private final String voxelName;

    public VoxelUri(String moduleId, String voxelName) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Module ID cannot be null or empty");
        }
        if (voxelName == null || voxelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Voxel name cannot be null or empty");
        }
        this.moduleId = moduleId.trim();
        this.voxelName = voxelName.trim();
    }

    public VoxelUri(String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) {
            throw new IllegalArgumentException("URI string cannot be null or empty");
        }

        String[] parts = uriString.trim().split(SEPARATOR, 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid URI format. Expected 'module:voxelname'");
        }

        this.moduleId = parts[0].trim();
        this.voxelName = parts[1].trim();

        if (moduleId.isEmpty() || voxelName.isEmpty()) {
            throw new IllegalArgumentException("Module ID and voxel name cannot be empty");
        }
    }

    @Override
    public String toString() {
        return moduleId + SEPARATOR + voxelName;
    }

    public static VoxelUri parse(String uriString) {
        return new VoxelUri(uriString);
    }
}
