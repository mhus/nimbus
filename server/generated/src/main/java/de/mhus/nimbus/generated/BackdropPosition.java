package de.mhus.nimbus.generated;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from Backdrop.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackdropPosition {

    /**
     * cx
     */
    private double cx;

    /**
     * cz
     */
    private double cz;

    /**
     * directions
     */
    private java.util.List<BackdropDirection> directions;
}
