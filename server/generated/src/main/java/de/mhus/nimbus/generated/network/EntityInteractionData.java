package de.mhus.nimbus.generated.network;

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
public class EntityInteractionData {

    /**
     * entityId
     */
    private String entityId;

    /**
     * ts
     */
    private double ts;

    /**
     * ac
     */
    private String ac;

    /**
     * pa (optional)
     */
    private java.util.Map<String, Object> pa;
}
