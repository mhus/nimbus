/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface VisibilityModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class VisibilityModifier {
    private Shape shape;
    private BlockEffect effect;
    private String effectParameters;
    private java.util.List<java.lang.Double> offsets;
    private java.lang.Double scalingX;
    private java.lang.Double scalingY;
    private java.lang.Double scalingZ;
    private java.lang.Double rotationX;
    private java.lang.Double rotationY;
    private String path;
    private java.util.Map<java.lang.Double, Object> textures;
    private FaceVisibility faceVisibility;
}
