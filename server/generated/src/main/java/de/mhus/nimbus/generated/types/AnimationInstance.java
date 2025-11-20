/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationInstance'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class AnimationInstance extends Object {
    private String templateId;
    private AnimationData animation;
    private double createdAt;
    private String triggeredBy;
}
