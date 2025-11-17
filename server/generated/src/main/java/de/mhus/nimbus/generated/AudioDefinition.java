package de.mhus.nimbus.generated;

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
public class AudioDefinition {

    /**
     * type
     */
    private AudioType | string type;

    /**
     * path
     */
    private String path;

    /**
     * volume
     */
    private double volume;

    /**
     * loop (optional)
     */
    private boolean loop;

    /**
     * enabled
     */
    private boolean enabled;

    /**
     * maxDistance (optional)
     */
    private double maxDistance;
}
