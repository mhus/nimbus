/*
 * Source TS: EntityMessage.ts
 * Original TS: 'interface EntityPositionUpdateData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityPositionUpdateData {
    @Deprecated
    @SuppressWarnings("required")
    private String pl;
    @Deprecated
    @SuppressWarnings("optional")
    private de.mhus.nimbus.evaluate.generated.types.Vector3 p;
    @Deprecated
    @SuppressWarnings("optional")
    private de.mhus.nimbus.evaluate.generated.types.Rotation r;
    @Deprecated
    @SuppressWarnings("optional")
    private de.mhus.nimbus.evaluate.generated.types.Vector3 v;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double po;
    @Deprecated
    @SuppressWarnings("required")
    private double ts;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> ta;
}
