package de.mhus.nimbus.server.shared.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Data Transfer Object for login requests.
 * Contains user credentials for authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDto {

    /**
     * User ID or login name
     */
    private String userId;

    /**
     * Plain text password
     */
    private String password;
}
