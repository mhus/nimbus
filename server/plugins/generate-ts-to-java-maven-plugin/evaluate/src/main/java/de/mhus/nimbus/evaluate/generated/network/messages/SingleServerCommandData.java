/*
 * Source TS: ServerCommandMessage.ts
 * Original TS: 'interface SingleServerCommandData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class SingleServerCommandData {
    @Deprecated
    @SuppressWarnings("required")
    private String cmd;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<String> args;
}
