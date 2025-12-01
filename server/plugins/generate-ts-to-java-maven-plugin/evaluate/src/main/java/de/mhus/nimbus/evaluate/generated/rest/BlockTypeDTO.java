/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockTypeDTO {
    private double id;
    private String name;
    private String displayName;
    private String shape;
    private String texture;
    private BlockTypeOptionsDTO options;
    private double hardness;
    private double miningtime;
    private String tool;
    private boolean unbreakable;
    private boolean solid;
    private boolean transparent;
    private double windLeafiness;
    private double windStability;
}
