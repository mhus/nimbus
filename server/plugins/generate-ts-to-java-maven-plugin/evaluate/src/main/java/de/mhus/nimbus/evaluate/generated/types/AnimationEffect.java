/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationEffect'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationEffect {
    private String id;
    private AnimationEffectType type;
    private java.util.List<PositionRef> positions;
    private java.util.Map<String, Object> params;
    private double startTime;
    private java.lang.Double duration;
    private java.lang.Double endTime;
    private java.lang.Boolean blocking;
}
