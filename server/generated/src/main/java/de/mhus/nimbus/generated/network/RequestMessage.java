/*
 * Source TS: BaseMessage.ts
 * Original TS: 'interface RequestMessage'
 */
package de.mhus.nimbus.generated.network;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RequestMessage extends BaseMessage {
    private String i;
}
