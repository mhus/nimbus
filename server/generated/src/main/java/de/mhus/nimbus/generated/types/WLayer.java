/*
 * Source TS: WLayer.ts
 * Original TS: 'interface WLayer'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WLayer {
    private String id;
    @com.fasterxml.jackson.annotation.JsonProperty("worldId")
    private String worldId;
    private String name;
    @com.fasterxml.jackson.annotation.JsonProperty("layerType")
    private LayerType layerType;
    @com.fasterxml.jackson.annotation.JsonProperty("layerDataId")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String layerDataId;
    @com.fasterxml.jackson.annotation.JsonProperty("mountX")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double mountX;
    @com.fasterxml.jackson.annotation.JsonProperty("mountY")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double mountY;
    @com.fasterxml.jackson.annotation.JsonProperty("mountZ")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double mountZ;
    private boolean ground;
    @com.fasterxml.jackson.annotation.JsonProperty("allChunks")
    private boolean allChunks;
    @com.fasterxml.jackson.annotation.JsonProperty("affectedChunks")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<String> affectedChunks;
    private double order;
    private boolean enabled;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.Map<java.lang.Double, String> groups;
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String createdAt;
    @com.fasterxml.jackson.annotation.JsonProperty("updatedAt")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String updatedAt;
}
