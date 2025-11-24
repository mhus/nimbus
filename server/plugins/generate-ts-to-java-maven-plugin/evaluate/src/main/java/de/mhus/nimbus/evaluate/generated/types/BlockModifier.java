/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface BlockModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockModifier {
    @Deprecated
    @SuppressWarnings("optional")
    private VisibilityModifier visibility;
    @Deprecated
    @SuppressWarnings("optional")
    private WindModifier wind;
    @Deprecated
    @SuppressWarnings("optional")
    private IlluminationModifier illumination;
    @Deprecated
    @SuppressWarnings("optional")
    private PhysicsModifier physics;
    @Deprecated
    @SuppressWarnings("optional")
    private EffectsModifier effects;
    @Deprecated
    @SuppressWarnings("optional")
    private AudioModifier audio;
}
