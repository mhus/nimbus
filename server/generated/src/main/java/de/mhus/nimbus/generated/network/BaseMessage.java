/*
 * Source TS: BaseMessage.ts
 * Original TS: 'interface BaseMessage'
 */
package de.mhus.nimbus.generated.network;

@lombok.Data
@lombok.Builder
public class BaseMessage extends Object {
    private String i;
    private String r;
    private MessageType t;
    private Object d;
}
