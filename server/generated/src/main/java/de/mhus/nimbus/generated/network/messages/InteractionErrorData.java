/*
 * Source TS: InteractionMessage.ts
 * Original TS: 'interface InteractionErrorData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class InteractionErrorData extends Object {
    private boolean success;
    private double errorCode;
    private String errorMessage;
}
