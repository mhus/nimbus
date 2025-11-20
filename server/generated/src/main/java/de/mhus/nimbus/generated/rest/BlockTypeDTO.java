/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeDTO'
 */
package de.mhus.nimbus.generated.rest;

@lombok.Data
@lombok.Builder
public class BlockTypeDTO extends Object {
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
