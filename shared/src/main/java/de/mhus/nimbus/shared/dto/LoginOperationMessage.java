package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginOperationMessage {

    private String requestId;
    private String username;
    private String password;
    private String clientInfo;
    private Long timestamp;
    private boolean rememberMe = false;
}
