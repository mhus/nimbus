package de.mhus.nimbus.worldbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.*;
import de.mhus.nimbus.worldbridge.model.WebSocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldBridgeServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private WorldService worldService;

    @InjectMocks
    private WorldBridgeService worldBridgeService;

    private WebSocketSession testSession;

    @BeforeEach
    void setUp() {
        testSession = new WebSocketSession();
    }

    @Test
    void testLoginSuccess() {
        // Given
        String sessionId = "test-session";
        LoginCommandData loginData = new LoginCommandData("valid-token");
        WebSocketCommand command = new WebSocketCommand("bridge", "login", loginData, "req-1");

        AuthenticationResult authResult = new AuthenticationResult(true, "user-1", Set.of("USER"), "testuser");
        when(authenticationService.validateToken("valid-token")).thenReturn(authResult);

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("success", response.getStatus());
        assertEquals("bridge", response.getService());
        assertEquals("login", response.getCommand());
        assertEquals("req-1", response.getRequestId());
        assertEquals("user-1", testSession.getUserId());
        assertTrue(testSession.getRoles().contains("USER"));
    }

    @Test
    void testLoginInvalidToken() {
        // Given
        String sessionId = "test-session";
        LoginCommandData loginData = new LoginCommandData("invalid-token");
        WebSocketCommand command = new WebSocketCommand("bridge", "login", loginData, "req-1");

        AuthenticationResult authResult = new AuthenticationResult(false, null, Set.of(), null);
        when(authenticationService.validateToken("invalid-token")).thenReturn(authResult);

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("error", response.getStatus());
        assertEquals("INVALID_TOKEN", response.getErrorCode());
        assertNull(testSession.getUserId());
    }

    @Test
    void testUseWorldSuccess() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");

        UseWorldCommandData useWorldData = new UseWorldCommandData("world-1");
        WebSocketCommand command = new WebSocketCommand("bridge", "use", useWorldData, "req-1");

        when(worldService.hasWorldAccess("user-1", "world-1")).thenReturn(true);
        when(worldService.getWorldDetails("world-1")).thenReturn("world-details");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("success", response.getStatus());
        assertEquals("world-1", testSession.getWorldId());
        assertEquals("world-details", response.getData());
    }

    @Test
    void testUseWorldNoAccess() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");

        UseWorldCommandData useWorldData = new UseWorldCommandData("world-1");
        WebSocketCommand command = new WebSocketCommand("bridge", "use", useWorldData, "req-1");

        when(worldService.hasWorldAccess("user-1", "world-1")).thenReturn(false);

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("error", response.getStatus());
        assertEquals("NO_WORLD_ACCESS", response.getErrorCode());
        assertNull(testSession.getWorldId());
    }

    @Test
    void testPing() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");

        PingCommandData pingData = new PingCommandData(System.currentTimeMillis());
        WebSocketCommand command = new WebSocketCommand("bridge", "ping", pingData, "req-1");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("success", response.getStatus());
        assertEquals("pong", response.getCommand());
        assertEquals(pingData.getTimestamp(), ((PingCommandData) response.getData()).getTimestamp());
    }

    @Test
    void testRegisterCluster() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");
        testSession.setWorldId("world-1");

        RegisterClusterCommandData.ClusterCoordinate cluster = new RegisterClusterCommandData.ClusterCoordinate(0, 0, 0);
        RegisterClusterCommandData clusterData = new RegisterClusterCommandData(List.of(cluster));
        WebSocketCommand command = new WebSocketCommand("bridge", "registerCluster", clusterData, "req-1");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("success", response.getStatus());
        assertEquals(1, testSession.getRegisteredClusters().size());
        assertEquals(0, testSession.getRegisteredClusters().get(0).getX());
    }

    @Test
    void testRegisterTerrain() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");
        testSession.setWorldId("world-1");

        RegisterTerrainCommandData terrainData = new RegisterTerrainCommandData(List.of("world", "group"));
        WebSocketCommand command = new WebSocketCommand("bridge", "registerTerrain", terrainData, "req-1");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("success", response.getStatus());
        assertEquals(2, testSession.getRegisteredTerrainEvents().size());
        assertTrue(testSession.getRegisteredTerrainEvents().contains("world"));
        assertTrue(testSession.getRegisteredTerrainEvents().contains("group"));
    }

    @Test
    void testCommandWithoutLogin() {
        // Given
        String sessionId = "test-session";
        // Note: Session without userId means not logged in
        WebSocketCommand command = new WebSocketCommand("bridge", "ping", new PingCommandData(), "req-1");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        // The WorldBridgeService doesn't check authentication - that's done in the WebSocketHandler
        // So this test should expect success, as the service assumes valid input
        assertEquals("success", response.getStatus());
        assertEquals("pong", response.getCommand());
    }

    @Test
    void testCommandWithoutWorld() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");
        // Note: Session without worldId means no world selected

        RegisterClusterCommandData clusterData = new RegisterClusterCommandData(List.of());
        WebSocketCommand command = new WebSocketCommand("bridge", "registerCluster", clusterData, "req-1");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        // The WorldBridgeService doesn't check world selection - that's done in the WebSocketHandler
        // So this test should expect success, as the service assumes valid input
        assertEquals("success", response.getStatus());
        assertEquals(0, testSession.getRegisteredClusters().size());
    }

    @Test
    void testUnknownCommand() {
        // Given
        String sessionId = "test-session";
        testSession.setUserId("user-1");
        testSession.setWorldId("world-1");

        WebSocketCommand command = new WebSocketCommand("bridge", "unknown", null, "req-1");

        // When
        WebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("error", response.getStatus());
        assertEquals("UNKNOWN_COMMAND", response.getErrorCode());
    }
}
