/*
 * Source TS: BlockMetadataDTO.ts
 * Original TS: 'interface BlockMetadataDTO'
 */
package de.mhus.nimbus.generated.rest;

@lombok.Data
@lombok.Builder
public class BlockMetadataDTO extends Object {
    private double x;
    private double y;
    private double z;
    private String id;
    private java.util.List<String> groups;
    private java.util.List<String> groupNames;
    private java.util.List<String> inheritedGroups;
    private java.util.List<String> inheritedGroupNames;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String displayName;
}
