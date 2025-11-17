package de.mhus.nimbus.generated.network;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockInteractionData {

    /**
     * x
     */
    private double x;

    /**
     * y
     */
    private double y;

    /**
     * z
     */
    private double z;

    /**
     * id (optional)
     */
    private String id;

    /**
     * gId (optional)
     */
    private String gId;

    /**
     * ac
     */
    private String ac;

    /**
     * pa (optional)
     */
    private java.util.Map<String, Object> pa;
}
