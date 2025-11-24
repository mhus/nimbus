/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationInstance'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationInstance {
    @Deprecated
    @SuppressWarnings("required")
    private String templateId;
    @Deprecated
    @SuppressWarnings("required")
    private AnimationData animation;
    @Deprecated
    @SuppressWarnings("required")
    private double createdAt;
    @Deprecated
    @SuppressWarnings("optional")
    private String triggeredBy;
}
