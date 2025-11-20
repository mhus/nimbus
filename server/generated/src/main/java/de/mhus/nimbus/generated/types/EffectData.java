/*
 * Source TS: EffectData.ts
 * Original TS: 'interface EffectData'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class EffectData extends Object {
    private String n;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double intensity;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String color;
}
