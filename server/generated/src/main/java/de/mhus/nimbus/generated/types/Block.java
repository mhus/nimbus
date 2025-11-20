/*
 * Source TS: Block.ts
 * Original TS: 'interface Block'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class Block extends Object {
    private Vector3 position;
    private double blockTypeId;
    private Offsets offsets;
    private java.util.List<Object> cornerHeights;
    private FaceVisibility faceVisibility;
    private java.lang.Double status;
    private java.util.Map<java.lang.Double, BlockModifier> modifiers;
    private BlockMetadata metadata;
}
