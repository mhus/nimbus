/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface TextureDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TextureDefinition {
    private String path;
    private UVMapping uvMapping;
    private SamplingMode samplingMode;
    private TransparencyMode transparencyMode;
    private java.lang.Double opacity;
    private BlockEffect effect;
    private String effectParameters;
    private String color;
    private java.lang.Boolean backFaceCulling;
}
