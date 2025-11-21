/*
 * Source TS: PingMessage.ts
 * Original TS: 'interface PongData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class PongData extends Object {
    private double cTs;
    private double sTs;
}
