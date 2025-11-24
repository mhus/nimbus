/*
 * Source TS: CommandMessage.ts
 * Original TS: 'interface CommandData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CommandData {
    @Deprecated
    @SuppressWarnings("required")
    private String cmd;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<String> args;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean oneway;
}
