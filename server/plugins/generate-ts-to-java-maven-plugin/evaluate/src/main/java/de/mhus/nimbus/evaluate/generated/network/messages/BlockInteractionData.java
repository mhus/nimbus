/*
 * Source TS: BlockMessage.ts
 * Original TS: 'interface BlockInteractionData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockInteractionData {
    @Deprecated
    @SuppressWarnings("required")
    private double x;
    @Deprecated
    @SuppressWarnings("required")
    private double y;
    @Deprecated
    @SuppressWarnings("required")
    private double z;
    @Deprecated
    @SuppressWarnings("optional")
    private String id;
    @Deprecated
    @SuppressWarnings("optional")
    private String gId;
    @Deprecated
    @SuppressWarnings("required")
    private String ac;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> pa;
}
