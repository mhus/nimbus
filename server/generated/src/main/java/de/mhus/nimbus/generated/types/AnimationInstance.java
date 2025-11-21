/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationInstance'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class AnimationInstance extends Object {
    private String templateId;
    private AnimationData animation;
    private double createdAt;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String triggeredBy;
}
