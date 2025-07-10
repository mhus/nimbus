package de.mhus.nimbus.entrance.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.common.service.SecurityService;
import de.mhus.nimbus.shared.dto.AuthenticationRequest;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import de.mhus.nimbus.entrance.service.ClientSessionService;
import de.mhus.nimbus.entrance.service.MessageDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test für NimbusWebSocketHandler
 */
@ExtendWith(MockitoExtension.class)
class NimbusWebSocketHandlerTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private ClientSessionService clientSessionService;

    @Mock
    private MessageDispatcher messageDispatcher;

    @Mock
    private WebSocketSession webSocketSession;

    private ObjectMapper objectMapper;
    private NimbusWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new NimbusWebSocketHandler(securityService, clientSessionService, messageDispatcher, objectMapper);
    }

    @Test
    void testConnectionEstablished() throws Exception {
        // Given
        when(webSocketSession.getId()).thenReturn("test-session-id");

        // When
        handler.afterConnectionEstablished(webSocketSession);

        // Then
        verify(clientSessionService).addSession(webSocketSession);
    }

    @Test
    void testSuccessfulAuthentication() throws Exception {
        // Given
        String sessionId = "test-session-id";
        when(webSocketSession.getId()).thenReturn(sessionId);

        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("testpass");

        WebSocketMessage message = WebSocketMessage.builder()
            .type("authenticate")
            .requestId("req-123")
            .data(authRequest)
            .build();

        SecurityService.LoginResult loginResult = mock(SecurityService.LoginResult.class);
        when(loginResult.isSuccess()).thenReturn(true);
        when(loginResult.getToken()).thenReturn("jwt-token");

        when(securityService.login("testuser", "testpass")).thenReturn(loginResult);

        String messageJson = objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(messageJson);

        // When
        handler.handleTextMessage(webSocketSession, textMessage);

        // Then
        verify(clientSessionService).authenticateSession(sessionId, "jwt-token", "testuser");
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void testConnectionClosed() throws Exception {
        // Given
        String sessionId = "test-session-id";
        when(webSocketSession.getId()).thenReturn(sessionId);

        // When
        handler.afterConnectionClosed(webSocketSession, null);

        // Then
        verify(clientSessionService).removeSession(sessionId);
    }
}
