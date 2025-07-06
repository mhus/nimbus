package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for planet registration operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanetRegistrationMessage {

    private String requestId;
    private String planetId;
    private String planetName;
    private String description;
    private String serverAddress;
    private Integer serverPort;
    private String version;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private String planetType;
    private String status;
    private Long timestamp;
}
