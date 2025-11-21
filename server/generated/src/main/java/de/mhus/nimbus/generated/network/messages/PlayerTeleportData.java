/*
 * Source TS: UserMessage.ts
 * Original TS: 'interface PlayerTeleportData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class PlayerTeleportData extends Object {
    private double x;
    private double y;
    private double z;
    private de.mhus.nimbus.generated.types.Rotation r;
}
