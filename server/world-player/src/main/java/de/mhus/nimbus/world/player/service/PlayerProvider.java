package de.mhus.nimbus.world.player.service;

import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerId;

import java.util.Optional;

public interface PlayerProvider {

    Optional<PlayerData> getPlayer(PlayerId playerId, ClientType clientType);
}
