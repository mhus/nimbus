/*
 * Source TS: BlockMessage.ts
 * Original TS: 'interface BlockInteractionData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockInteractionData {
    private double x;
    private double y;
    private double z;
    private String id;
    private String gId;
    private String ac;
    private java.util.Map<String, Object> pa;
}
