package de.mhus.nimbus.world.player.service;

import de.mhus.nimbus.generated.configs.Settings;
import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.shared.types.PlayerCharacter;
import de.mhus.nimbus.shared.types.PlayerData;
import de.mhus.nimbus.shared.types.PlayerId;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.player.session.SessionPingConsumer;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.region.RCharacter;
import de.mhus.nimbus.world.shared.region.RCharacterService;
import de.mhus.nimbus.world.shared.region.RUserItemsService;
import de.mhus.nimbus.world.shared.sector.RUser;
import de.mhus.nimbus.world.shared.sector.RUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerService implements SessionPingConsumer {

    private final RUserService rUserService;
    private final RCharacterService rCharacterService;
    private final RUserItemsService rUserItemsService;
    private final WorldRedisService worldRedisService;

    /**
     * Get player data for a specific region.
     */
    public Optional<PlayerData> getPlayer(PlayerId playerId, ClientType clientType, String regionId) {
        String username = playerId.getUserId();
        String characterId = playerId.getCharacterId();

        // Get user from database
        Optional<RUser> userOpt = rUserService.getByUsername(username);
        if (userOpt.isEmpty()) {
            log.debug("User not found: {}", username);
            return Optional.empty();
        }

        RUser rUser = userOpt.get();

        // Get character from database
        Optional<RCharacter> characterOpt = rCharacterService.getCharacter(username, regionId, characterId);
        if (characterOpt.isEmpty()) {
            log.debug("Character not found: userId={}, regionId={}, characterId={}", username, regionId, characterId);
            return Optional.empty();
        }

        RCharacter rCharacter = characterOpt.get();

        // Get Settings for ClientType
        Settings settings = rUserService.getSettingsForClientType(username, clientType.getTsIndex());
        if (settings == null) {
            settings = new Settings(); // default empty settings
        }

        // Build PlayerCharacter
        PlayerCharacter playerCharacter = new PlayerCharacter(rCharacter.getPublicData(), rCharacter.getBackpack() );

        return Optional.of(new PlayerData(rUser.getPublicData(), playerCharacter, settings));
    }

    @Override
    public ACTION onSessionPing(PlayerSession session) {
        // TODO sync player data if needed, validate only one session etc.
        return ACTION.NONE;
    }

    public Optional<Entity> getPlayerAsEntity(PlayerId playerId, WorldId worldId) {
        // Extract regionId from worldId
        String regionId = worldId.getRegionId();

        var playerX = getPlayer(playerId, ClientType.WEB, regionId);
        if (playerX.isEmpty()) return Optional.empty();
        var player = playerX.get();

        // Check if player has public data
        if (player.character() == null || player.character().getPublicData() == null) {
            log.warn("Player character or publicData is null for playerId: {}", playerId);
            return Optional.empty();
        }

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
//                .healthMax(500)
//                .health(400)
                .build();
        return Optional.of(result);
    }
}
