package de.mhus.nimbus.server.shared.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Data Transfer Object for creating a new user.
 * Contains all required information for user creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserDto {

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
     * Plain text password (will be hashed)
     */
    private String password;

    /**
     * List of roles assigned to the user
     */
    private List<String> roles;
}
