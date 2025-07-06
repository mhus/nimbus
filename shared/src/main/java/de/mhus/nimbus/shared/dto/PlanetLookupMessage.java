package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for planet lookup operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanetLookupMessage {

    private String requestId;
    private String planetId;
    private String planetName;
    private String planetType;
    private String status;
    private boolean includeInactive = false;
}
