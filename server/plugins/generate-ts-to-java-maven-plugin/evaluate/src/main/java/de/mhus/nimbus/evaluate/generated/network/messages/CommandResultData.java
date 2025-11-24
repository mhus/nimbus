/*
 * Source TS: CommandMessage.ts
 * Original TS: 'interface CommandResultData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CommandResultData {
    @Deprecated
    @SuppressWarnings("required")
    private double rc;
    @Deprecated
    @SuppressWarnings("required")
    private String message;
}
