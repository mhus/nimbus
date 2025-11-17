package de.mhus.nimbus.generated.network;

import de.mhus.nimbus.generated.types.Rotation;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from UserMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMovementData {

    /**
     * p (optional)
     */
    private java.util.Map<String, Object> p;

    /**
     * r (optional)
     */
    private Rotation r;
}
