/*
 * Source TS: BaseMessage.ts
 * Original TS: 'interface ResponseMessage'
 */
package de.mhus.nimbus.evaluate.generated.network;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ResponseMessage extends BaseMessage {
    @Deprecated
    @SuppressWarnings("required")
    private String r;
}
