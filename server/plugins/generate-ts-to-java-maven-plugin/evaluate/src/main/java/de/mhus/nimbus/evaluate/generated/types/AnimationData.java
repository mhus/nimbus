/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationData'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationData {
    @Deprecated
    @SuppressWarnings("optional")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double duration;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<AnimationEffect> effects;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<String> placeholders;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean loop;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double repeat;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> source;
}
