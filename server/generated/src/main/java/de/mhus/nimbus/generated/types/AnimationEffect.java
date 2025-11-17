package de.mhus.nimbus.generated.types;

import java.util.List;
import java.util.Map;
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
public class AnimationEffect {

    /**
     * id (optional)
     */
    private String id;

    /**
     * type
     */
    private AnimationEffectType type;

    /**
     * positions (optional)
     */
    private java.util.List<String> positions;

    /**
     * params
     */
    private java.util.Map<String, Object> params;

    /**
     * startTime
     */
    private double startTime;

    /**
     * duration (optional)
     */
    private double duration;

    /**
     * endTime (optional)
     */
    private double endTime;

    /**
     * blocking (optional)
     */
    private boolean blocking;
}
