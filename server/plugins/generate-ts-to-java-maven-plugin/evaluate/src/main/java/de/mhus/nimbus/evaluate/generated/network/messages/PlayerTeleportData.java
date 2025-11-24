/*
 * Source TS: UserMessage.ts
 * Original TS: 'interface PlayerTeleportData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerTeleportData {
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, Object> p;
    @Deprecated
    @SuppressWarnings("required")
    private de.mhus.nimbus.evaluate.generated.types.Rotation r;
}
