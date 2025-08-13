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
}
