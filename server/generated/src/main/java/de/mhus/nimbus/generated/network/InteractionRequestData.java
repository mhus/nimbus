package de.mhus.nimbus.generated.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from InteractionMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionRequestData {

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
     * g (optional)
     */
    private String g;
}
