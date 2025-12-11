package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.world.player.session.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameplayService {
    public void onPlayerEntityInteraction(PlayerSession session, String entityId, String action, Long timestamp, JsonNode params) {
        log.info("Player {} interacted with entity {}: action={}, timestamp={}, params={}",
                session.getPlayer(), entityId, action, timestamp, params);
    }

    public void onPlayerBlockInteraction(PlayerSession session, int x, int y, int z, String blockId, String groupId, String action, JsonNode params) {
        log.info("Player {} interacted with block at ({}, {}, {}): blockId={}, groupId={}, action={}, params={}",
                session.getPlayer(), x, y, z, blockId, groupId, action, params);
    }
}
