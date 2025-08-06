package de.mhus.nimbus.server.shared.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Data Transfer Object for JWT token responses.
 * Contains the JWT token and expiration information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenDto {

    /**
     * JWT token string
     */
    private String token;

    /**
     * Token expiration time (Unix timestamp)
     */
    private Long expiresAt;

    /**
     * Token issue time (Unix timestamp)
     */
    private Long issuedAt;
}
