package de.mhus.nimbus.worldbridge.command;

import de.mhus.nimbus.worldbridge.model.WebSocketSession;
import de.mhus.nimbus.shared.dto.websocket.WebSocketCommand;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteRequest {
    private String sessionId;
    private WebSocketSession sessionInfo;
    private WebSocketCommand command;
}
