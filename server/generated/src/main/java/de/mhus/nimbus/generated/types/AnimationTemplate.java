/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationTemplate'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class AnimationTemplate extends Object {
    private String id;
    private java.util.List<String> placeholders;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
}
