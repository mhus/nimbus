/*
 * Source TS: BlockMessage.ts
 * Original TS: 'interface BlockStatusUpdate'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class BlockStatusUpdate extends Object {
    private double x;
    private double y;
    private double z;
    private double s;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.AnimationData> aa;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.AnimationData> ab;
}
