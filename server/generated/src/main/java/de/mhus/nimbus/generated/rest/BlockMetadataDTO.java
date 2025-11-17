package de.mhus.nimbus.generated.rest;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockMetadataDTO.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockMetadataDTO {

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
     * id
     */
    private String id;

    /**
     * groups
     */
    private java.util.List<String> groups;

    /**
     * groupNames
     */
    private java.util.List<String> groupNames;

    /**
     * inheritedGroups
     */
    private java.util.List<String> inheritedGroups;

    /**
     * inheritedGroupNames
     */
    private java.util.List<String> inheritedGroupNames;

    /**
     * displayName (optional)
     */
    private String displayName;
}
