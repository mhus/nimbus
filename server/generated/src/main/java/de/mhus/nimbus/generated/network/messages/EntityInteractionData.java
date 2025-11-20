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
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.Map<String, Object> pa;
}
