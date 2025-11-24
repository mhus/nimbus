/*
 * Source TS: BaseMessage.ts
 * Original TS: 'interface BaseMessage'
 */
package de.mhus.nimbus.evaluate.generated.network;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BaseMessage {
    @Deprecated
    @SuppressWarnings("optional")
    private String i;
    @Deprecated
    @SuppressWarnings("optional")
    private String r;
    @Deprecated
    @SuppressWarnings("required")
    private MessageType t;
    @Deprecated
    @SuppressWarnings("optional")
    private Object d;
}
