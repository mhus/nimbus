/*
 * Source TS: EntityMessage.ts
 * Original TS: 'interface EntityInteractionData'
 */
package de.mhus.nimbus.evaluate.generated;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityInteractionData {
    private String entityId;
    private long ts;
    private String ac;
    private java.util.Map<String, Object> pa;
}
