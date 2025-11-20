package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class TextureDefinition extends Object {
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
