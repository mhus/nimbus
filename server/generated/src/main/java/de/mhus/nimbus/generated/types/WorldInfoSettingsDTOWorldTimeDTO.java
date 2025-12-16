/*
 * Source TS: World.ts
 * Original TS: 'interface WorldInfoSettingsDTOWorldTimeDTO'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldInfoSettingsDTOWorldTimeDTO {
    @com.fasterxml.jackson.annotation.JsonProperty("minuteScaling")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double minuteScaling;
    @com.fasterxml.jackson.annotation.JsonProperty("minutesPerHour")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double minutesPerHour;
    @com.fasterxml.jackson.annotation.JsonProperty("hoursPerDay")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double hoursPerDay;
    @com.fasterxml.jackson.annotation.JsonProperty("daysPerMonth")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double daysPerMonth;
    @com.fasterxml.jackson.annotation.JsonProperty("monthsPerYear")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double monthsPerYear;
    @com.fasterxml.jackson.annotation.JsonProperty("yearsPerEra")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double yearsPerEra;
    @com.fasterxml.jackson.annotation.JsonProperty("currentEra")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double currentEra;
    @com.fasterxml.jackson.annotation.JsonProperty("linuxEpocheDeltaMinutes")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double linuxEpocheDeltaMinutes;
    @com.fasterxml.jackson.annotation.JsonProperty("daySections")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private WorldInfoSettingsDTOWorldTimeDTODaySectionsDTO daySections;
    @com.fasterxml.jackson.annotation.JsonProperty("celestialBodies")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private WorldInfoSettingsDTOWorldTimeDTOCelestialBodiesDTO celestialBodies;
}
