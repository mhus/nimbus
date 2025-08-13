package de.mhus.nimbus.world.bridge.service;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResult {
    private boolean valid;
    private String userId;
    private Set<String> roles;
    private String username;
}
