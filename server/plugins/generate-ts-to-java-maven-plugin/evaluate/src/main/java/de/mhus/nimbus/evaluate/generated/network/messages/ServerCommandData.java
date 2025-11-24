/*
 * Source TS: ServerCommandMessage.ts
 * Original TS: 'interface ServerCommandData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ServerCommandData {
    @Deprecated
    @SuppressWarnings("optional")
    private String cmd;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<String> args;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean oneway;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<SingleServerCommandData> cmds;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean parallel;
}
