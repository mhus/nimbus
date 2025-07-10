package de.mhus.nimbus.shared.util;

import de.mhus.nimbus.shared.avro.AvroVoxel;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelInstance;
import de.mhus.nimbus.shared.voxel.VoxelType;
import io.vavr.control.Try;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility-Klasse für die Konvertierung zwischen VoxelInstance und AvroVoxel
 */
public class VoxelConverter {

    /**
     * Konvertiert ein VoxelInstance-Objekt zu einem AvroVoxel-Objekt
     *
     * @param voxelInstance Das zu konvertierende VoxelInstance
     * @return Das konvertierte AvroVoxel
     */
    public static AvroVoxel toAvroVoxel(VoxelInstance voxelInstance) {
        if (voxelInstance == null) {
            return null;
        }

        AvroVoxel.Builder builder = AvroVoxel.newBuilder()
                .setX(voxelInstance.getX())
                .setY(voxelInstance.getY())
                .setZ(voxelInstance.getZ());

        // VoxelType konvertieren
        if (voxelInstance.getVoxelType() != null) {
            Voxel voxelType = voxelInstance.getVoxelType();
            builder.setVoxelType(voxelType.getDisplayName());
            builder.setMaterial(voxelType.getDisplayName()); // Verwende displayName als Material
            builder.setHealth((float) voxelType.getHardness());
            builder.setX(voxelInstance.getX());
            builder.setY(voxelInstance.getY());
            builder.setZ(voxelInstance.getZ());

            // Einfache Tags basierend auf Voxel-Eigenschaften
            if (voxelType.isLiquid()) {
                builder.setTags(List.of("liquid"));
            }
            if (voxelType.isAttachmentAllowed()) {
                builder.setTags(List.of("attachmentAllowed"));
            }
            if (voxelType.isReplacementAllowed()) {
                builder.setTags(List.of("replacementAllowed"));
            }

            // Properties können basierend auf Voxel-Eigenschaften gesetzt werden
            // Hier eine einfache Implementierung
        }

        return builder.build();
    }

    /**
     * Konvertiert ein AvroVoxel-Objekt zu einem VoxelInstance-Objekt
     *
     * @param avroVoxel Das zu konvertierende AvroVoxel
     * @return Das konvertierte VoxelInstance
     */
    public static VoxelInstance fromAvroVoxel(AvroVoxel avroVoxel) {
        if (avroVoxel == null) {
            return null;
        }

        // Erstelle einen einfachen Voxel-Typ basierend auf den AvroVoxel-Daten
        Voxel voxelType = null;
        if (avroVoxel.getVoxelType() != null) {
            voxelType = Voxel.builder()
                    .displayName(avroVoxel.getVoxelType())
                    .id( Try.of(() -> (short)VoxelType.valueOf(avroVoxel.getVoxelType().toUpperCase()).getId()).getOrElse((short) VoxelType.AIR.getId())) // Fallback auf 0, wenn der Typ nicht gefunden wird
                    .hardness(avroVoxel.getHealth() != null ? avroVoxel.getHealth().intValue() : 3)
                    .liquid(avroVoxel.getTags() != null && avroVoxel.getTags().contains("liquid"))
                    .x(avroVoxel.getX())
                    .y(avroVoxel.getY())
                    .z(avroVoxel.getZ())
                    .build();
        }

        return new VoxelInstance(avroVoxel.getX(), avroVoxel.getY(), avroVoxel.getZ(), voxelType);
    }

    /**
     * Konvertiert eine Liste von VoxelInstance-Objekten zu einer Liste von AvroVoxel-Objekten
     *
     * @param voxelInstances Die zu konvertierende VoxelInstance-Liste
     * @return Die konvertierte AvroVoxel-Liste
     */
    public static List<AvroVoxel> toAvroVoxelList(List<VoxelInstance> voxelInstances) {
        if (voxelInstances == null) {
            return null;
        }
        return voxelInstances.stream()
                .map(VoxelConverter::toAvroVoxel)
                .collect(Collectors.toList());
    }

    /**
     * Konvertiert eine Liste von AvroVoxel-Objekten zu einer Liste von VoxelInstance-Objekten
     *
     * @param avroVoxels Die zu konvertierende AvroVoxel-Liste
     * @return Die konvertierte VoxelInstance-Liste
     */
    public static List<VoxelInstance> fromAvroVoxelList(List<AvroVoxel> avroVoxels) {
        if (avroVoxels == null) {
            return null;
        }
        return avroVoxels.stream()
                .map(VoxelConverter::fromAvroVoxel)
                .collect(Collectors.toList());
    }
}
