/*
 * Source TS: CommandMessage.ts
 * Original TS: 'interface CommandData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class CommandData extends Object {
    private String cmd;
    private java.util.List<String> args;
    private java.lang.Boolean oneway;
}
