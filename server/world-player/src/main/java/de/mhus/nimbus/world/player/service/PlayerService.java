package de.mhus.nimbus.world.player.service;

import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.generated.types.EntityModifier;
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

    @Override
    public ACTION onSessionPing(PlayerSession session) {
        // TODO sync player data if needed using PlayerProvider, validate only one session etc.
        return ACTION.NONE;
    }

    public Optional<Entity> getPlayerAsEntity(PlayerId playerId, String worldId) {
        var playerX = getPlayer(playerId, ClientType.WEB);
        if (playerX.isEmpty()) return Optional.empty();
        var player = playerX.get();
        var result = Entity.builder()
                .id(playerId.getId())
                .name(player.character().getPublicData().getDisplayName())
                .controlledBy("player")
                .model(player.character().getPublicData().getThirdPersonModelId())
                .clientPhysics(false)
                .modelModifier(
                        player.character().getPublicData().getThirdPersonModelModifiers()
                )
                .interactive(true)
                .movementType("dynamic")
                .physics(false)
                .notifyOnAttentionRange(player.character().getPublicData().getStealthRange())
                .notifyOnCollision(true)
                .build();
        return Optional.of(result);
    }
}
