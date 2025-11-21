/*
 * Source TS: CommandMessage.ts
 * Original TS: 'interface CommandResultData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class CommandResultData extends Object {
    private double rc;
    private String message;
}
