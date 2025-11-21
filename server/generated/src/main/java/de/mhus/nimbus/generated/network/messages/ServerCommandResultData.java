/*
 * Source TS: ServerCommandMessage.ts
 * Original TS: 'interface ServerCommandResultData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class ServerCommandResultData extends Object {
    private double rc;
    private String message;
}
