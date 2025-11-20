/*
 * Source TS: UserMessage.ts
 * Original TS: 'interface UserMovementData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class UserMovementData extends Object {
    private double x;
    private double y;
    private double z;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private de.mhus.nimbus.generated.types.Rotation r;
}
