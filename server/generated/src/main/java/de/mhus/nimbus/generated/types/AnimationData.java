/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationData'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class AnimationData extends Object {
    private String id;
    private String name;
    private java.lang.Double duration;
    private java.util.List<AnimationEffect> effects;
    private java.util.List<String> placeholders;
    private java.lang.Boolean loop;
    private java.lang.Double repeat;
    private String type;
    private String playerId;
}
