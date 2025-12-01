/*
 * Source TS: Block.ts
 * Original TS: 'interface Block'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Block {
    private Vector3 position;
    private double blockTypeId;
    private Offsets offsets;
    private java.util.List<Object> cornerHeights;
    private FaceVisibility faceVisibility;
    private java.lang.Double status;
    private java.util.Map<java.lang.Double, BlockModifier> modifiers;
    private BlockMetadata metadata;
}
