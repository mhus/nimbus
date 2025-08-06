package de.mhus.nimbus.server.shared.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Data Transfer Object for User information.
 * Used for API requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    /**
     * Unique identifier and login name of the user
     */
    private String id;

    /**
     * Full name of the user
     */
    private String name;

    /**
     * Nickname of the user
     */
    private String nickname;

    /**
     * Email address of the user
     */
    private String email;

    /**
     * List of roles assigned to the user
     */
    private List<String> roles;

    /**
     * Timestamp when the user was created (Unix time)
     */
    private Long createdAt;

    /**
     * Timestamp when the user was last updated (Unix time)
     */
    private Long updatedAt;
}
