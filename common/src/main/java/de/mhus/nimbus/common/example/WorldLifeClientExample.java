package de.mhus.nimbus.common.example;

import de.mhus.nimbus.common.client.WorldLifeClient;
import de.mhus.nimbus.shared.character.CharacterType;
import de.mhus.nimbus.shared.dto.CharacterOperationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Beispiel-Service, der zeigt, wie der WorldLifeClient verwendet wird
 */
@Service
public class WorldLifeClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeClientExample.class);

    private final WorldLifeClient worldLifeClient;

    @Autowired
    public WorldLifeClientExample(WorldLifeClient worldLifeClient) {
        this.worldLifeClient = worldLifeClient;
    }

    /**
     * Beispiel: Erstellt einen neuen Spieler-Charakter
     */
    public CompletableFuture<Void> createPlayerExample(String worldId, String playerName, double x, double y, double z) {
        LOGGER.info("Erstelle Spieler '{}' an Position ({}, {}, {}) in Welt {}", playerName, x, y, z, worldId);

        return worldLifeClient.createCharacterWithDetails(
                worldId,
                CharacterType.PLAYER,
                x, y, z,
                playerName,
                "Spieler " + playerName,
                "Ein Spieler-Charakter",
                100, // Gesundheit
                100  // Max-Gesundheit
            )
            .thenRun(() -> LOGGER.info("Spieler '{}' erfolgreich erstellt", playerName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Erstellen des Spielers '{}': {}", playerName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Erstellt einen NPC
     */
    public CompletableFuture<Void> createNpcExample(String worldId, String npcName, double x, double y, double z) {
        LOGGER.info("Erstelle NPC '{}' an Position ({}, {}, {}) in Welt {}", npcName, x, y, z, worldId);

        return worldLifeClient.createCharacterWithDetails(
                worldId,
                CharacterType.NPC,
                x, y, z,
                npcName,
                "Händler " + npcName,
                "Ein freundlicher Händler",
                50, // Gesundheit
                50  // Max-Gesundheit
            )
            .thenRun(() -> LOGGER.info("NPC '{}' erfolgreich erstellt", npcName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Erstellen des NPCs '{}': {}", npcName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Bewegt einen Charakter zu einer neuen Position
     */
    public CompletableFuture<Void> moveCharacterExample(String worldId, Long characterId, double newX, double newY, double newZ) {
        LOGGER.info("Bewege Charakter {} zu Position ({}, {}, {}) in Welt {}", characterId, newX, newY, newZ, worldId);

        return worldLifeClient.updateCharacterPosition(worldId, characterId, newX, newY, newZ)
            .thenRun(() -> LOGGER.info("Charakter {} erfolgreich bewegt", characterId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Bewegen des Charakters {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Reduziert die Gesundheit eines Charakters (Schaden)
     */
    public CompletableFuture<Void> damageCharacterExample(String worldId, Long characterId, int damage) {
        LOGGER.info("Füge Charakter {} {} Schaden zu in Welt {}", characterId, damage, worldId);

        // Hier würde normalerweise die aktuelle Gesundheit abgefragt werden
        // Für das Beispiel nehmen wir an, dass der Charakter 50 Gesundheit hat
        int currentHealth = 50;
        int newHealth = Math.max(0, currentHealth - damage);

        return worldLifeClient.updateCharacterHealth(worldId, characterId, newHealth)
            .thenRun(() -> LOGGER.info("Charakter {} hat jetzt {} Gesundheit", characterId, newHealth))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Aktualisieren der Gesundheit von Charakter {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Heilt einen Charakter
     */
    public CompletableFuture<Void> healCharacterExample(String worldId, Long characterId, int healAmount) {
        LOGGER.info("Heile Charakter {} um {} Punkte in Welt {}", characterId, healAmount, worldId);

        // Für das Beispiel nehmen wir an, dass der Charakter 30 Gesundheit hat
        int currentHealth = 30;
        int maxHealth = 100;
        int newHealth = Math.min(maxHealth, currentHealth + healAmount);

        return worldLifeClient.updateCharacterHealth(worldId, characterId, newHealth)
            .thenRun(() -> LOGGER.info("Charakter {} wurde geheilt und hat jetzt {} Gesundheit", characterId, newHealth))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Heilen von Charakter {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Aktualisiert Charakter-Informationen
     */
    public CompletableFuture<Void> updateCharacterInfoExample(String worldId, Long characterId, String newName, String newDisplayName) {
        LOGGER.info("Aktualisiere Informationen für Charakter {} in Welt {}", characterId, worldId);

        return worldLifeClient.updateCharacterInfo(worldId, characterId, newName, newDisplayName, "Aktualisierte Beschreibung")
            .thenRun(() -> LOGGER.info("Charakter-Informationen für {} erfolgreich aktualisiert", characterId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Aktualisieren der Charakter-Informationen für {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Erstellt mehrere Tiere in einem Batch
     */
    public CompletableFuture<Void> spawnAnimalsExample(String worldId, int count) {
        LOGGER.info("Spawne {} Tiere in Welt {}", count, worldId);

        List<CharacterOperationMessage.CharacterData> animals = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CharacterOperationMessage.CharacterData animal = new CharacterOperationMessage.CharacterData();
            animal.setCharacterType(CharacterType.ANIMAL);
            animal.setName("Tier_" + i);
            animal.setDisplayName("Wildes Tier " + i);
            animal.setDescription("Ein wildes Tier in der Natur");
            animal.setX(Math.random() * 100);
            animal.setY(64);
            animal.setZ(Math.random() * 100);
            animal.setHealth(25);
            animal.setMaxHealth(25);
            animal.setActive(true);

            animals.add(animal);
        }

        return worldLifeClient.batchCreateCharacters(worldId, animals)
            .thenRun(() -> LOGGER.info("{} Tiere erfolgreich gespawnt", count))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Spawnen von Tieren: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Löscht einen Charakter
     */
    public CompletableFuture<Void> removeCharacterExample(String worldId, Long characterId) {
        LOGGER.info("Lösche Charakter {} aus Welt {}", characterId, worldId);

        return worldLifeClient.deleteCharacter(worldId, characterId)
            .thenRun(() -> LOGGER.info("Charakter {} erfolgreich gelöscht", characterId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Löschen des Charakters {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Deaktiviert einen Charakter (ohne zu löschen)
     */
    public CompletableFuture<Void> deactivateCharacterExample(String worldId, Long characterId) {
        LOGGER.info("Deaktiviere Charakter {} in Welt {}", characterId, worldId);

        return worldLifeClient.setCharacterActive(worldId, characterId, false)
            .thenRun(() -> LOGGER.info("Charakter {} erfolgreich deaktiviert", characterId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Deaktivieren des Charakters {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Komplexe Operation - Erstellt einen Charakter und bewegt ihn dann
     */
    public CompletableFuture<Void> createAndMoveCharacterExample(String worldId, String characterName,
                                                                double startX, double startY, double startZ,
                                                                double endX, double endY, double endZ) {
        LOGGER.info("Erstelle und bewege Charakter '{}' von ({}, {}, {}) zu ({}, {}, {}) in Welt {}",
                   characterName, startX, startY, startZ, endX, endY, endZ, worldId);

        return worldLifeClient.createCharacter(worldId, CharacterType.NPC,
                                             startX, startY, startZ, characterName)
            .thenCompose(result -> {
                LOGGER.info("Charakter erstellt, bewege nun...");
                // In einem echten Szenario würde man hier die ID des erstellten Charakters erhalten
                // Für das Beispiel nehmen wir eine fiktive ID
                Long characterId = 12345L;
                return worldLifeClient.updateCharacterPosition(worldId, characterId, endX, endY, endZ);
            })
            .thenRun(() -> LOGGER.info("Charakter '{}' erfolgreich erstellt und bewegt", characterName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der kombinierten Operation für '{}': {}", characterName, throwable.getMessage());
                return null;
            });
    }
}
