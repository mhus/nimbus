package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerData;
import de.mhus.nimbus.shared.types.PlayerId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalPlayerProvider implements PlayerProvider {

    @Value("${world.player.data.directory:./data/players}")
    private String playerDataDirectory;

    private final ObjectMapper objectMapper;

    @Override
    public Optional<PlayerData> getPlayer(PlayerId playerId, ClientType clientType) {

        String pathStr = playerDataDirectory + "/" + normalizePath(playerId.getRawId() + "_" + clientType.tsString()) + ".json";
        var path = Paths.get(pathStr);
        try {
            if (!Files.exists(path)) {
                log.debug("Player data file does not exist at path: {}", pathStr);
                return Optional.empty();
            }
            var content = Files.readString(path);
            var playerData = objectMapper.readValue(content, PlayerData.class);
            return Optional.of(playerData);
        } catch (IOException e) {
            log.warn("Player data not found for id: {} and client: {} at path: {}", playerId, clientType, pathStr);
            return Optional.empty();
        }
    }

    private String normalizePath(String path) {
        return path.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
    }
}
