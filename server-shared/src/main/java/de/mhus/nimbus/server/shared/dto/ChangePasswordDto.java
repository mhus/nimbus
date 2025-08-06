package de.mhus.nimbus.server.shared.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Data Transfer Object for password change requests.
 * Contains old and new password for verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordDto {

    /**
     * Current password for verification
     */
    private String oldPassword;

    /**
     * New password to set
     */
    private String newPassword;
}
