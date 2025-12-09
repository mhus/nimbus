package de.mhus.nimbus.world.player.service;

import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerData;
import de.mhus.nimbus.shared.types.PlayerId;
import de.mhus.nimbus.world.player.session.SessionPingConsumer;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerService implements SessionPingConsumer {

    private final PlayerProvider playerProvider;
    private final WorldRedisService worldRedisService;

    public Optional<PlayerData> getPlayer(PlayerId playerId, ClientType clientType) {
        return playerProvider.getPlayer(playerId, clientType);
    }

    public Optional<PlayerData> getPlayer(PlayerSession session) {
        // TODO get stored player from session
        return Optional.empty();
    }

    @Override
    public ACTION onSessionPing(PlayerSession session) {
        // TODO sync player data if needed using PlayerProvider, validate only one session etc.
        return ACTION.NONE;
    }
}
