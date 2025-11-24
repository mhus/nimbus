/*
 * Source TS: EntityMessage.ts
 * Original TS: 'interface EntityInteractionData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityInteractionData {
    @Deprecated
    @SuppressWarnings("required")
    private String entityId;
    @Deprecated
    @SuppressWarnings("required")
    private double ts;
    @Deprecated
    @SuppressWarnings("required")
    private String ac;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> pa;
}
