package de.mhus.nimbus.shared.voxel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Repräsentiert eine Voxel-Instanz mit Koordinaten und einem Voxel-Typ
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VoxelInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;
    private int z;

    // Der Voxel-Typ (Material/Template)
    private Voxel voxelType;

    // Zusätzliche Eigenschaften für diese spezifische Instanz
    private float health;

    /**
     * Konstruktor mit Koordinaten und Voxel-Typ
     */
    public VoxelInstance(int x, int y, int z, Voxel voxelType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.voxelType = voxelType;
        this.health = voxelType != null ? voxelType.getHardness() : 1.0f;
    }
}
