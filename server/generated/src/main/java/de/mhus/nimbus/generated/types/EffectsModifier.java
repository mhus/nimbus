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
public class EffectsModifier {

    /**
     * forceEgoView (optional)
     */
    private boolean forceEgoView;

    /**
     * sky (optional)
     */
    private SkyEffect sky;
}
