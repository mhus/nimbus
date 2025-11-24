/*
 * Source TS: PingMessage.ts
 * Original TS: 'interface PongMessage'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PongMessage extends de.mhus.nimbus.evaluate.generated.network.BaseMessage {
    @Deprecated
    @SuppressWarnings("required")
    private String r;
}
