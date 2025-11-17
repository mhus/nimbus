package de.mhus.nimbus.generated.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindModifier {

    /**
     * leafiness (optional)
     */
    private double leafiness;

    /**
     * stability (optional)
     */
    private double stability;

    /**
     * leverUp (optional)
     */
    private double leverUp;

    /**
     * leverDown (optional)
     */
    private double leverDown;
}
