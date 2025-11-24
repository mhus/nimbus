/*
 * Source TS: InteractionMessage.ts
 * Original TS: 'interface InteractionRequestData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InteractionRequestData {
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
    private String g;
}
