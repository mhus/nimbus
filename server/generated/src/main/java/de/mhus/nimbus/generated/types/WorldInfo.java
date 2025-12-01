/*
 * Source TS: World.ts
 * Original TS: 'interface WorldInfo'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldInfo {
    @com.fasterxml.jackson.annotation.JsonProperty("worldId")
    private String worldId;
    private String name;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Vector3 start;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Vector3 stop;
    @com.fasterxml.jackson.annotation.JsonProperty("chunkSize")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double chunkSize;
    @com.fasterxml.jackson.annotation.JsonProperty("assetPath")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String assetPath;
    @com.fasterxml.jackson.annotation.JsonProperty("assetPort")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double assetPort;
    @com.fasterxml.jackson.annotation.JsonProperty("worlRegion")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String worlRegion;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double status;
    @com.fasterxml.jackson.annotation.JsonProperty("seasonStatus")
    private SeasonStatus seasonStatus;
    @com.fasterxml.jackson.annotation.JsonProperty("seasonProgress")
    private double seasonProgress;
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    private String createdAt;
    @com.fasterxml.jackson.annotation.JsonProperty("updatedAt")
    private String updatedAt;
    private java.util.Map<String, Object> owner;
    private java.util.Map<String, Object> settings;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.Map<String, Object> license;
    @com.fasterxml.jackson.annotation.JsonProperty("startArea")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.Map<String, Object> startArea;
    @com.fasterxml.jackson.annotation.JsonProperty("editorUrl")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String editorUrl;
    @com.fasterxml.jackson.annotation.JsonProperty("splashScreen")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String splashScreen;
    @com.fasterxml.jackson.annotation.JsonProperty("splashScreenAudio")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String splashScreenAudio;
}
