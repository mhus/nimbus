package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for world registration operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorldRegistrationMessage {

    private String requestId;
    private String worldId;
    private String worldName;
    private String planetId;
    private String description;
    private String worldType;
    private String status;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private String configuration;
    private Long timestamp;
}
