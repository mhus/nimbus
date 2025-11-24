/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldSettingsDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldSettingsDTO {
    @Deprecated
    @SuppressWarnings("required")
    private double maxPlayers;
    @Deprecated
    @SuppressWarnings("required")
    private boolean allowGuests;
    @Deprecated
    @SuppressWarnings("required")
    private boolean pvpEnabled;
    @Deprecated
    @SuppressWarnings("required")
    private double pingInterval;
}
