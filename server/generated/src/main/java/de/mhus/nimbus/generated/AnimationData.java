package de.mhus.nimbus.generated;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from AnimationData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimationData {

    /**
     * id (optional)
     */
    private String id;

    /**
     * name
     */
    private String name;

    /**
     * duration (optional)
     */
    private double duration;

    /**
     * effects
     */
    private java.util.List<AnimationEffect> effects;

    /**
     * placeholders (optional)
     */
    private java.util.List<String> placeholders;

    /**
     * loop (optional)
     */
    private boolean loop;

    /**
     * repeat (optional)
     */
    private double repeat;

    /**
     * source (optional)
     */
    private { source;

    /**
     * type
     */
    private 'server' | 'client' type;

    /**
     * playerId (optional)
     */
    private String playerId;
}
