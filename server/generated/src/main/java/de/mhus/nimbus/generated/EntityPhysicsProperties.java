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
public class EntityPhysicsProperties {

    /**
     * mass
     */
    private double mass;

    /**
     * friction
     */
    private double friction;

    /**
     * restitution (optional)
     */
    private double restitution;

    /**
     * drag (optional)
     */
    private double drag;
}
