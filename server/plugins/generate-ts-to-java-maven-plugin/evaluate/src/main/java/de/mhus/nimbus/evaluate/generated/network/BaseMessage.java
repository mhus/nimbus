/*
 * Source TS: BaseMessage.ts
 * Original TS: 'interface BaseMessage'
 */
package de.mhus.nimbus.evaluate.generated.network;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BaseMessage {
    private String i;
    private String r;
    private MessageType t;
    private Object d;
}
