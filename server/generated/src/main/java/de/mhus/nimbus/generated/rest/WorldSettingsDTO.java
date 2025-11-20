package de.mhus.nimbus.generated.rest;

@lombok.Data
@lombok.Builder
public class WorldSettingsDTO extends Object {
    private double maxPlayers;
    private boolean allowGuests;
    private boolean pvpEnabled;
    private double pingInterval;
}
