/*
 * Source TS: InteractionMessage.ts
 * Original TS: 'interface InteractionErrorData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InteractionErrorData {
    private boolean success;
    @com.fasterxml.jackson.annotation.JsonProperty("errorCode")
    private double errorCode;
    @com.fasterxml.jackson.annotation.JsonProperty("errorMessage")
    private String errorMessage;
}
