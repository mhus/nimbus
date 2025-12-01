/*
 * Source TS: EntityMessage.ts
 * Original TS: 'interface EntityPositionUpdateData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityPositionUpdateData {
    private String pl;
    private de.mhus.nimbus.evaluate.generated.types.Vector3 p;
    private de.mhus.nimbus.evaluate.generated.types.Rotation r;
    private de.mhus.nimbus.evaluate.generated.types.Vector3 v;
    private java.lang.Double po;
    private double ts;
    private java.util.Map<String, Object> ta;
}
