package de.mhus.nimbus.world.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified user information DTO for list operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto {

    private String id;
    private String username;
    private String email;
    private boolean enabled;
}
