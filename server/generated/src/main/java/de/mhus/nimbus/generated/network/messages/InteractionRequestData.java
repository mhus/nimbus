/*
 * Source TS: InteractionMessage.ts
 * Original TS: 'interface InteractionRequestData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class InteractionRequestData extends Object {
    private double x;
    private double y;
    private double z;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String g;
}
