package de.mhus.nimbus.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from EntityData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseAnimation {

    /**
     * animationName
     */
    private String animationName;

    /**
     * speedMultiplier
     */
    private double speedMultiplier;

    /**
     * loop
     */
    private boolean loop;
}
