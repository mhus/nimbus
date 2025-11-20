package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class BlockModifier extends Object {
    private VisibilityModifier visibility;
    private WindModifier wind;
    private IlluminationModifier illumination;
    private PhysicsModifier physics;
    private EffectsModifier effects;
    private AudioModifier audio;
}
