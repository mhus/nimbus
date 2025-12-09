package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerId;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EcbBladeLoadTest {

    @Test
    public void testLoadEcbBlade() throws Exception {
        // Test the JSON file directly
        String playerDataDirectory = "./data/players";
        ObjectMapper objectMapper = new ObjectMapper();

        PlayerId playerId = PlayerId.of("@ecb:blade").orElseThrow();
        ClientType clientType = ClientType.WEB;

        // Build the expected filename
        String normalizedId = playerId.getRawId().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        String filename = normalizedId + "_" + clientType.tsString() + ".json";
        String pathStr = playerDataDirectory + "/" + filename;

        System.out.println("Looking for file: " + pathStr);
        System.out.println("File exists: " + Files.exists(Paths.get(pathStr)));

        // Check if ecb_blade.json exists (without client type suffix)
        String directPath = playerDataDirectory + "/ecb_blade.json";
        System.out.println("Direct file path: " + directPath);
        System.out.println("Direct file exists: " + Files.exists(Paths.get(directPath)));

        if (Files.exists(Paths.get(directPath))) {
            String content = Files.readString(Paths.get(directPath));
            System.out.println("File content length: " + content.length());
            System.out.println("First 200 chars: " + content.substring(0, Math.min(200, content.length())));
        }

        // Test LocalPlayerProvider
        LocalPlayerProvider provider = new LocalPlayerProvider(objectMapper);
        // Use reflection to set the directory
        java.lang.reflect.Field field = LocalPlayerProvider.class.getDeclaredField("playerDataDirectory");
        field.setAccessible(true);
        field.set(provider, playerDataDirectory);

        var result = provider.getPlayer(playerId, clientType);
        System.out.println("Player loaded: " + result.isPresent());

        if (result.isPresent()) {
            System.out.println("User ID: " + result.get().user().getUserId());
            System.out.println("Display Name: " + result.get().user().getDisplayName());
        }
    }
}
