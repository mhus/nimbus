/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface TextureDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TextureDefinition {
    @Deprecated
    @SuppressWarnings("required")
    private String path;
    @Deprecated
    @SuppressWarnings("optional")
    private UVMapping uvMapping;
    @Deprecated
    @SuppressWarnings("optional")
    private SamplingMode samplingMode;
    @Deprecated
    @SuppressWarnings("optional")
    private TransparencyMode transparencyMode;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double opacity;
    @Deprecated
    @SuppressWarnings("optional")
    private BlockEffect effect;
    @Deprecated
    @SuppressWarnings("optional")
    private String effectParameters;
    @Deprecated
    @SuppressWarnings("optional")
    private String color;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean backFaceCulling;
}
