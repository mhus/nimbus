package de.mhus.nimbus.generated;

import java.util.Map;
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
public class Entity {

    /**
     * id
     */
    private String id;

    /**
     * name
     */
    private String name;

    /**
     * modelModifier
     */
    private java.util.Map<String, Object> modelModifier;

    /**
     * modifier (optional)
     */
    private EntityModifier modifier;

    /**
     * movementType
     */
    private String movementType;

    /**
     * controlledBy
     */
    private String controlledBy;

    /**
     * solid (optional)
     */
    private boolean solid;

    /**
     * interactive (optional)
     */
    private boolean interactive;

    /**
     * physics (optional)
     */
    private boolean physics;

    /**
     * clientPhysics (optional)
     */
    private boolean clientPhysics;

    /**
     * notifyOnCollision (optional)
     */
    private boolean notifyOnCollision;

    /**
     * notifyOnAttentionRange (optional)
     */
    private double notifyOnAttentionRange;
}
