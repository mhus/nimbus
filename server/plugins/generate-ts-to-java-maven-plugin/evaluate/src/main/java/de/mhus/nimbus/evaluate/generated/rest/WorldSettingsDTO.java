/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldSettingsDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldSettingsDTO {
    private double maxPlayers;
    private boolean allowGuests;
    private boolean pvpEnabled;
    private double pingInterval;
}
