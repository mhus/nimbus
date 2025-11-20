/*
 * Source TS: ServerCommandMessage.ts
 * Original TS: 'interface SingleServerCommandData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class SingleServerCommandData extends Object {
    private String cmd;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<String> args;
}
