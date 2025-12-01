/*
 * Source TS: UserMessage.ts
 * Original TS: 'interface PlayerTeleportData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerTeleportData {
    private java.util.Map<String, Object> p;
    private de.mhus.nimbus.evaluate.generated.types.Rotation r;
}
