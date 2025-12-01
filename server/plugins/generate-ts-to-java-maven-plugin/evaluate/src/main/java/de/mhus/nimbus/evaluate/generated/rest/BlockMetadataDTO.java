/*
 * Source TS: BlockMetadataDTO.ts
 * Original TS: 'interface BlockMetadataDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

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
    private java.util.List<String> groupNames;
    private java.util.List<String> inheritedGroups;
    private java.util.List<String> inheritedGroupNames;
    private String displayName;
}
