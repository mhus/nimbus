/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationTemplate'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationTemplate extends AnimationData {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<String> placeholders;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
}
