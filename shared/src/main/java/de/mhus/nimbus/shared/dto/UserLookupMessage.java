package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user lookup operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLookupMessage {

    private String requestId;
    private Long userId;
    private String username;
    private String email;
    private boolean includeInactive = false;
}
