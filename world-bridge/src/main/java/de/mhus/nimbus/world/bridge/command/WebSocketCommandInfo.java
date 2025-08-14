package de.mhus.nimbus.world.bridge.command;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketCommandInfo {
    private String service;
    private String command;
    private String description;
    private boolean isWorldRequired;

    // Convenience constructor for backward compatibility
    public WebSocketCommandInfo(String service, String command, String description) {
        this.service = service;
        this.command = command;
        this.description = description;
        this.isWorldRequired = true; // Default: world is required
    }
}
