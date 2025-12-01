/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface BlockModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockModifier {
    private VisibilityModifier visibility;
    private WindModifier wind;
    private IlluminationModifier illumination;
    private PhysicsModifier physics;
    private EffectsModifier effects;
    private AudioModifier audio;
}
