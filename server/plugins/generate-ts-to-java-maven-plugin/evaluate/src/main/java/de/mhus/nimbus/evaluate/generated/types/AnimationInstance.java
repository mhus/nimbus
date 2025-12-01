/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationInstance'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationInstance {
    private String templateId;
    private AnimationData animation;
    private double createdAt;
    private String triggeredBy;
}
