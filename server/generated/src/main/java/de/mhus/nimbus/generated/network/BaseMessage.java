/*
 * Source TS: BaseMessage.ts
 * Original TS: 'interface BaseMessage'
 */
package de.mhus.nimbus.generated.network;

@lombok.Data
@lombok.Builder
public class BaseMessage extends Object {
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String i;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String r;
    private MessageType t;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Object d;
}
