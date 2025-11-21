/*
 * Source TS: PingMessage.ts
 * Original TS: 'interface PongMessage'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
public class PongMessage extends de.mhus.nimbus.generated.network.BaseMessage {
    private String r;
}
