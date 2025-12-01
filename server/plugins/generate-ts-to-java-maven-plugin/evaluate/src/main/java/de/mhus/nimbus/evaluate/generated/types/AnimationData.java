/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationData'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationData {
    private String id;
    private String name;
    private java.lang.Double duration;
    private java.util.List<AnimationEffect> effects;
    private java.util.List<String> placeholders;
    private java.lang.Boolean loop;
    private java.lang.Double repeat;
    private java.util.Map<String, Object> source;
}
