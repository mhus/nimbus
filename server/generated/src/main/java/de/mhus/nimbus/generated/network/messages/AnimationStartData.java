/*
 * Source TS: AnimationMessage.ts
 * Original TS: 'interface AnimationStartData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class AnimationStartData extends Object {
    private double x;
    private double y;
    private double z;
    private de.mhus.nimbus.generated.types.AnimationData animation;
}
