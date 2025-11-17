package de.mhus.nimbus.generated;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ItemData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemData {

    /**
     * block
     */
    private Block block;

    /**
     * description (optional)
     */
    private String description;

    /**
     * pose (optional)
     */
    private String pose;

    /**
     * wait (optional)
     */
    private double wait;

    /**
     * duration (optional)
     */
    private double duration;

    /**
     * parameters (optional)
     */
    private java.util.Map<String, Object> parameters;
}
