/*
 * Source TS: AnimationMessage.ts
 * Original TS: 'interface AnimationStartData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationStartData {
    private double x;
    private double y;
    private double z;
    private de.mhus.nimbus.evaluate.generated.types.AnimationData animation;
}
