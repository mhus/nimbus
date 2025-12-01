/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface WorldSettingsDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldSettingsDTO {
    @com.fasterxml.jackson.annotation.JsonProperty("maxPlayers")
    private double maxPlayers;
    @com.fasterxml.jackson.annotation.JsonProperty("allowGuests")
    private boolean allowGuests;
    @com.fasterxml.jackson.annotation.JsonProperty("pvpEnabled")
    private boolean pvpEnabled;
    @com.fasterxml.jackson.annotation.JsonProperty("pingInterval")
    private double pingInterval;
}
