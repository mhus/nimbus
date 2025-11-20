package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class PlayerMovementStateChangedEvent extends Object {
    private String playerId;
    private PlayerMovementState oldState;
    private PlayerMovementState newState;
}
