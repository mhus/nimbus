/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationEffect'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationEffect {
    @Deprecated
    @SuppressWarnings("optional")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private AnimationEffectType type;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<PositionRef> positions;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, Object> params;
    @Deprecated
    @SuppressWarnings("required")
    private double startTime;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double duration;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double endTime;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean blocking;
}
