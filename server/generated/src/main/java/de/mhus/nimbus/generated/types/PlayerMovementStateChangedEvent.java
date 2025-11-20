package de.mhus.nimbus.generated.types;

public interface PlayerMovementStateChangedEvent {
    String getPlayerId();
    PlayerMovementState getOldState();
    PlayerMovementState getNewState();
}
