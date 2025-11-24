/*
 * Source TS: AnimationMessage.ts
 * Original TS: 'interface AnimationStartData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationStartData {
    @Deprecated
    @SuppressWarnings("required")
    private double x;
    @Deprecated
    @SuppressWarnings("required")
    private double y;
    @Deprecated
    @SuppressWarnings("required")
    private double z;
    @Deprecated
    @SuppressWarnings("required")
    private de.mhus.nimbus.evaluate.generated.types.AnimationData animation;
}
