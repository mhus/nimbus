/*
 * Source TS: BlockMetadataDTO.ts
 * Original TS: 'interface BlockMetadataDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockMetadataDTO {
    private double x;
    private double y;
    private double z;
    private String id;
    private java.util.List<String> groups;
    @com.fasterxml.jackson.annotation.JsonProperty("groupNames")
    private java.util.List<String> groupNames;
    @com.fasterxml.jackson.annotation.JsonProperty("inheritedGroups")
    private java.util.List<String> inheritedGroups;
    @com.fasterxml.jackson.annotation.JsonProperty("inheritedGroupNames")
    private java.util.List<String> inheritedGroupNames;
    @com.fasterxml.jackson.annotation.JsonProperty("displayName")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String displayName;
}
