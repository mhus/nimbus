package de.mhus.nimbus.world.bridge.command;

import de.mhus.nimbus.world.bridge.model.WebSocketSession;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteRequest {
    private String sessionId;
    private WebSocketSession sessionInfo;
    private WorldWebSocketCommand command;
}
