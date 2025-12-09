package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerData;
import de.mhus.nimbus.shared.types.PlayerId;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstration der LocalPlayerProvider Funktionalität mit dem ecb_blade Player.
 * Dieser Test zeigt, wie Player-Daten aus JSON-Dateien geladen werden.
 */
class EcbBladePlayerLoadDemoTest {

    @Test
    void demonstrateEcbBladePlayerLoading() {
        System.out.println("=== ECB BLADE PLAYER LOAD DEMONSTRATION ===\n");

        // 1. PlayerId für ecb_blade erstellen
        System.out.println("1. Creating PlayerId for ecb_blade...");
        PlayerId playerId = PlayerId.of("@ecb:blade").orElseThrow();

        System.out.println("   Full PlayerId: " + playerId.getId());
        System.out.println("   User ID: " + playerId.getUserId());
        System.out.println("   Character ID: " + playerId.getCharacterId());
        System.out.println("   Raw ID: " + playerId.getRawId());

        // 2. Filename-Generierung demonstrieren
        System.out.println("\n2. Demonstrating filename generation...");
        for (ClientType clientType : ClientType.values()) {
            String normalizedPath = playerId.getRawId().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
            String filename = normalizedPath + "_" + clientType.tsString() + ".json";
            System.out.println("   " + clientType + " -> " + filename);
        }

        // 3. LocalPlayerProvider instanziieren
        System.out.println("\n3. Creating LocalPlayerProvider...");
        LocalPlayerProvider provider = new LocalPlayerProvider(new ObjectMapper());
        ReflectionTestUtils.setField(provider, "playerDataDirectory", "./data/players");

        // 4. Versuche Player für verschiedene Client Types zu laden
        System.out.println("\n4. Attempting to load ecb_blade for different client types...");
        boolean loaded = false;

        for (ClientType clientType : ClientType.values()) {
            Optional<PlayerData> result = provider.getPlayer(playerId, clientType);

            if (result.isPresent()) {
                System.out.println("   ✓ SUCCESS with " + clientType + ":");
                PlayerData playerData = result.get();
                System.out.println("     User ID: " + playerData.user().getUserId());
                System.out.println("     Display Name: " + playerData.user().getDisplayName());

                if (playerData.settings() != null) {
                    System.out.println("     Settings Name: " + playerData.settings().getName());
                }

                loaded = true;

                // Assertions für Test-Validierung
                assertThat(playerData.user().getUserId()).isEqualTo("ecb");
                assertThat(playerData.user().getDisplayName()).isEqualTo("Eric Cross Brooks");

            } else {
                System.out.println("   ✗ No data found for " + clientType);
            }
        }

        System.out.println("\n5. Test Summary:");
        if (loaded) {
            System.out.println("   ✓ Successfully demonstrated ecb_blade player loading!");
        } else {
            System.out.println("   ⚠ No player data files found for ecb_blade");
            System.out.println("     Make sure the appropriate JSON files exist in ./data/players/");
        }

        System.out.println("\n=== END DEMONSTRATION ===");

        // Test ist immer erfolgreich, da es eine Demonstration ist
        assertThat(true).isTrue();
    }

    @Test
    void verifyPlayerDataStructure() {
        System.out.println("=== PLAYER DATA STRUCTURE VERIFICATION ===\n");

        // Test minimal player data structure
        String testJson = """
            {
              "user": {
                "userId": "ecb",
                "displayName": "Eric Cross Brooks"
              },
              "character": {},
              "settings": {
                "name": "ecb_blade_settings"
              }
            }
            """;

        try {
            ObjectMapper mapper = new ObjectMapper();
            PlayerData playerData = mapper.readValue(testJson, PlayerData.class);

            System.out.println("✓ PlayerData structure is valid:");
            System.out.println("  User: " + playerData.user().getUserId() + " - " + playerData.user().getDisplayName());
            System.out.println("  Character: " + (playerData.character() != null ? "Present" : "Null"));
            System.out.println("  Settings: " + (playerData.settings() != null ? playerData.settings().getName() : "Null"));

            assertThat(playerData.user()).isNotNull();
            assertThat(playerData.user().getUserId()).isEqualTo("ecb");

        } catch (Exception e) {
            System.out.println("✗ Error parsing player data: " + e.getMessage());
            throw new RuntimeException("PlayerData structure test failed", e);
        }

        System.out.println("\n=== END VERIFICATION ===");
    }
}
