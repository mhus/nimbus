/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationTemplate'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
public class AnimationTemplate extends AnimationData {
    private String id;
    private java.util.List<String> placeholders;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
}
