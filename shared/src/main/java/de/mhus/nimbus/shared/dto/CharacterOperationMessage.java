package de.mhus.nimbus.shared.dto;

import de.mhus.nimbus.shared.character.CharacterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for character operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterOperationMessage {

    public enum OperationType {
        CREATE, UPDATE_POSITION, UPDATE_HEALTH, UPDATE_INFO, DELETE, BATCH_CREATE
    }

    private String messageId;
    private OperationType operation;
    private String worldId;
    private CharacterData characterData;
    private BatchData batchData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterData {
        private Long characterId;
        private CharacterType characterType;
        private double x, y, z;
        private String name;
        private String displayName;
        private String description;
        private Integer health;
        private Integer maxHealth;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchData {
        private String charactersJson; // JSON array of characters
    }
}
