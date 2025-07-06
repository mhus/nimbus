package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for identity character lookup operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityCharacterLookupMessage {

    private String requestId;
    private Long characterId;
    private String characterName;
    private Long userId;
    private String currentPlanet;
    private String currentWorldId;
    private boolean activeOnly = true;
}
