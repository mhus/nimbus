/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationTemplate'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationTemplate extends AnimationData {
    private String id;
    private java.util.List<String> placeholders;
    private String description;
}
