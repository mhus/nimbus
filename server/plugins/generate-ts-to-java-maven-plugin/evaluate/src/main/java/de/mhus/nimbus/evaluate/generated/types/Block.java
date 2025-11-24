/*
 * Source TS: Block.ts
 * Original TS: 'interface Block'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Block {
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 position;
    @Deprecated
    @SuppressWarnings("required")
    private double blockTypeId;
    @Deprecated
    @SuppressWarnings("optional")
    private Offsets offsets;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<Object> cornerHeights;
    @Deprecated
    @SuppressWarnings("optional")
    private FaceVisibility faceVisibility;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double status;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<java.lang.Double, BlockModifier> modifiers;
    @Deprecated
    @SuppressWarnings("optional")
    private BlockMetadata metadata;
}
