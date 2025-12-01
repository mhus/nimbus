/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockTypeDTO {
    private String id;
    private String name;
    private String displayName;
    private String shape;
    private String texture;
    private BlockTypeOptionsDTO options;
    private double hardness;
    private long miningtime;
    private String tool;
    private boolean unbreakable;
    private boolean solid;
    private boolean transparent;
    private double windLeafiness;
    private double windStability;
}
