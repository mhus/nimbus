/*
 * Source TS: PingMessage.ts
 * Original TS: 'interface PongData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PongData {
    @Deprecated
    @SuppressWarnings("required")
    private double cTs;
    @Deprecated
    @SuppressWarnings("required")
    private double sTs;
}
