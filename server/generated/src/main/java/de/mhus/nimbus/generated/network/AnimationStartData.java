package de.mhus.nimbus.generated.network;

import de.mhus.nimbus.generated.types.AnimationData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from AnimationMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimationStartData {

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
     * animation
     */
    private AnimationData animation;
}
