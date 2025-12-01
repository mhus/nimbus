/*
 * Source TS: AnimationData.ts
 * Original TS: 'interface AnimationInstance'
 */
package de.mhus.nimbus.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AnimationInstance {
    private String templateId;
    private AnimationData animation;
    private double createdAt;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String triggeredBy;
}
