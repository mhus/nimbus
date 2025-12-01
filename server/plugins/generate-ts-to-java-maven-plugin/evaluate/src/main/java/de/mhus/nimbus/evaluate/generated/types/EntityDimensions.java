/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityDimensions'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityDimensions {
    private java.util.Map<String, Object> walk;
    private java.util.Map<String, Object> sprint;
    private java.util.Map<String, Object> crouch;
    private java.util.Map<String, Object> swim;
    private java.util.Map<String, Object> climb;
    private java.util.Map<String, Object> fly;
    private java.util.Map<String, Object> teleport;
}
