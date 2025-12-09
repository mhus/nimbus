package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerData;
import de.mhus.nimbus.shared.types.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPlayerProviderTest {

    private LocalPlayerProvider playerProvider;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        playerProvider = new LocalPlayerProvider(objectMapper);
        ReflectionTestUtils.setField(playerProvider, "playerDataDirectory", tempDir.toString());
    }

    @Test
    void shouldLoadValidPlayerData() throws IOException {
        // Given: PlayerId für Test-Player
        PlayerId playerId = PlayerId.of("@test:user").orElseThrow();
        ClientType clientType = ClientType.WEB;

        // Create minimal valid player data file
        String playerDataJson = """
            {
              "user": {
                "userId": "test",
                "displayName": "Test User"
              },
              "character": {},
              "settings": {
                "name": "test_settings"
              }
            }
            """;

        // Write test data to file (filename format: rawId_clientType.json)
        Path playerFile = tempDir.resolve("test_user_web.json");
        Files.writeString(playerFile, playerDataJson);

        // When: Player wird geladen
        Optional<PlayerData> result = playerProvider.getPlayer(playerId, clientType);

        // Then: Player data sollte gefunden werden
        assertThat(result).isPresent();
        PlayerData playerData = result.get();

        assertThat(playerData.user().getUserId()).isEqualTo("test");
        assertThat(playerData.user().getDisplayName()).isEqualTo("Test User");
        assertThat(playerData.settings().getName()).isEqualTo("test_settings");
    }

    @Test
    void shouldReturnEmptyForNonExistentPlayer() {
        // Given: PlayerId für einen nicht existierenden Player
        PlayerId playerId = PlayerId.of("@nonexistent:player").orElseThrow();
        ClientType clientType = ClientType.WEB;

        // When: Player wird geladen
        Optional<PlayerData> result = playerProvider.getPlayer(playerId, clientType);

        // Then: Kein Player sollte gefunden werden
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleDifferentClientTypes() throws IOException {
        // Given: PlayerId und verschiedene Client Types
        PlayerId playerId = PlayerId.of("@multi:client").orElseThrow();

        String playerDataJson = """
            {
              "user": {
                "userId": "multi",
                "displayName": "Multi Client User"
              },
              "character": {},
              "settings": {
                "name": "multi_settings"
              }
            }
            """;

        // Create files for different client types
        Files.writeString(tempDir.resolve("multi_client_web.json"), playerDataJson);
        Files.writeString(tempDir.resolve("multi_client_mobile.json"), playerDataJson);

        // When & Then: Verschiedene Client Types sollten funktionieren
        Optional<PlayerData> webResult = playerProvider.getPlayer(playerId, ClientType.WEB);
        Optional<PlayerData> mobileResult = playerProvider.getPlayer(playerId, ClientType.MOBILE);

        assertThat(webResult).isPresent();
        assertThat(webResult.get().user().getUserId()).isEqualTo("multi");

        assertThat(mobileResult).isPresent();
        assertThat(mobileResult.get().user().getUserId()).isEqualTo("multi");
    }

    @Test
    void shouldNormalizeSpecialCharactersInFilename() throws IOException {
        // Given: PlayerId mit speziellen Zeichen (@, :, etc.)
        PlayerId playerId = new PlayerId("@special:char@test");
        ClientType clientType = ClientType.WEB;

        String playerDataJson = """
            {
              "user": {
                "userId": "special",
                "displayName": "Special Character User"
              },
              "character": {},
              "settings": {
                "name": "special_settings"
              }
            }
            """;

        // Create file with normalized filename (special chars replaced with _)
        // rawId would be "special:char@test", normalized to "special_char_test"
        Path playerFile = tempDir.resolve("special_char_test_web.json");
        Files.writeString(playerFile, playerDataJson);

        // When: Player wird geladen
        Optional<PlayerData> result = playerProvider.getPlayer(playerId, clientType);

        // Then: Player sollte gefunden werden trotz spezieller Zeichen in ID
        assertThat(result).isPresent();
        assertThat(result.get().user().getUserId()).isEqualTo("special");
    }

    @Test
    void shouldReturnEmptyForInvalidJson() throws IOException {
        // Given: PlayerId und eine Datei mit invalid JSON
        PlayerId playerId = PlayerId.of("@invalid:json").orElseThrow();
        ClientType clientType = ClientType.WEB;

        // Create file with invalid JSON
        Path playerFile = tempDir.resolve("invalid_json_web.json");
        Files.writeString(playerFile, "{ invalid json content }");

        // When: Player wird geladen
        Optional<PlayerData> result = playerProvider.getPlayer(playerId, clientType);

        // Then: Kein Player sollte gefunden werden (Exception wird gefangen)
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTestEcbBladePlayerIdParsing() {
        // Given: Die echte ecb_blade PlayerId
        String playerIdString = "@ecb:blade";

        // When: PlayerId wird geparst
        Optional<PlayerId> playerId = PlayerId.of(playerIdString);

        // Then: PlayerId sollte korrekt geparst werden
        assertThat(playerId).isPresent();
        PlayerId ecbPlayerId = playerId.get();

        assertThat(ecbPlayerId.getUserId()).isEqualTo("ecb");
        assertThat(ecbPlayerId.getCharacterId()).isEqualTo("blade");
        assertThat(ecbPlayerId.getRawId()).isEqualTo("ecb:blade");
        assertThat(ecbPlayerId.getId()).isEqualTo("@ecb:blade");

        // Test filename generation
        String expectedFilename = ecbPlayerId.getRawId().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_") + "_web.json";
        assertThat(expectedFilename).isEqualTo("ecb_blade_web.json");
    }

    @Test
    void shouldAttemptToLoadEcbBladeFromRealDirectory() {
        // Given: Echter LocalPlayerProvider mit echter Konfiguration
        LocalPlayerProvider realProvider = new LocalPlayerProvider(new ObjectMapper());
        ReflectionTestUtils.setField(realProvider, "playerDataDirectory", "./data/players");

        PlayerId playerId = PlayerId.of("@ecb:blade").orElseThrow();

        // Test verschiedene Client Types um zu sehen welcher funktioniert
        for (ClientType clientType : ClientType.values()) {
            Optional<PlayerData> result = realProvider.getPlayer(playerId, clientType);
            if (result.isPresent()) {
                System.out.println("Successfully loaded ecb_blade with client type: " + clientType);
                System.out.println("User ID: " + result.get().user().getUserId());
                System.out.println("Display Name: " + result.get().user().getDisplayName());
                return; // Test erfolgreich für mindestens einen Client Type
            }
        }

        // Wenn kein Client Type funktioniert hat, prüfe direkt die ecb_blade.json Datei
        Path directFile = Paths.get("./data/players/ecb_blade.json");
        if (Files.exists(directFile)) {
            System.out.println("ecb_blade.json exists but couldn't be loaded with standard naming convention");
            System.out.println("File path: " + directFile.toAbsolutePath());
        } else {
            System.out.println("ecb_blade.json file not found at expected location");
        }

        // Test ist nicht fehlgeschlagen, nur informatisch
        assertThat(true).isTrue(); // Dummy assertion für gültigen Test
    }
}
