package de.mhus.nimbus.generated.network;

import de.mhus.nimbus.generated.types.Rotation;
import de.mhus.nimbus.generated.types.Vector3;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from EntityMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityPositionUpdateData {

    /**
     * pl
     */
    private String pl;

    /**
     * p (optional)
     */
    private Vector3 p;

    /**
     * r (optional)
     */
    private Rotation r;

    /**
     * v (optional)
     */
    private Vector3 v;

    /**
     * po (optional)
     */
    private double po;

    /**
     * ts
     */
    private double ts;

    /**
     * ta (optional)
     */
    private java.util.Map<String, Object> ta;
}
