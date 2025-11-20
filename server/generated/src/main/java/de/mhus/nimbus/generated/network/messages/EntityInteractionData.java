/*
 * Source TS: EntityMessage.ts
 * Original TS: 'interface EntityInteractionData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class EntityInteractionData extends Object {
    private String entityId;
    private double ts;
    private String ac;
    private java.util.Map<String, Object> pa;
}
