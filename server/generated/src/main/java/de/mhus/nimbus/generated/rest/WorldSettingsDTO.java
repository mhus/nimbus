/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldSettingsDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class WorldSettingsDTO extends Object {
    private double maxPlayers;
    private boolean allowGuests;
    private boolean pvpEnabled;
    private double pingInterval;
}
