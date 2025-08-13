package de.mhus.nimbus.world.bridge.websocket;

import de.mhus.nimbus.world.bridge.service.AuthenticationResult;
import de.mhus.nimbus.world.bridge.service.AuthenticationService;
import de.mhus.nimbus.world.bridge.service.WorldService;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "nimbus.identity.service.url=http://localhost:8080",
    "nimbus.world.terrain.service.url=http://localhost:8081"
})
class WorldBridgeWebSocketHandlerIntegrationTest {

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private WorldService worldService;

    @MockBean
    private TerrainServiceClient terrainServiceClient;

    @Test
    void contextLoads() {
        // Mock authentication service for context loading
        AuthenticationResult authResult = new AuthenticationResult(true, "user-1", Set.of("USER"), "testuser");
        when(authenticationService.validateToken(anyString())).thenReturn(authResult);
        when(worldService.hasWorldAccess(anyString(), anyString())).thenReturn(true);
        when(worldService.getWorldDetails(anyString())).thenReturn("world-details");

        // Test passes if context loads successfully
    }
}
