package de.mhus.nimbus.world.editor.api;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.world.shared.world.WWorldService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.world.shared.world.WWorld;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UserWorldEditorControllerTest.MockConfig.class)
class UserWorldEditorControllerTest {

    @TestConfiguration
    static class MockConfig {
        @Bean JwtService jwtService() { return Mockito.mock(JwtService.class); }
        @Bean WWorldService worldService() { return Mockito.mock(WWorldService.class); }
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired WWorldService worldService;

    String token = "dummy";
    String userId = "user123";

    @BeforeEach
    void setupJwt() {
        Claims claims = Mockito.mock(Claims.class);
        Mockito.when(claims.getSubject()).thenReturn(userId);
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = (Jws<Claims>) Mockito.mock(Jws.class);
        Mockito.when(jws.getPayload()).thenReturn(claims);
        Mockito.when(jwtService.validateTokenWithPublicKey(Mockito.eq(token), Mockito.eq(KeyType.UNIVERSE), Mockito.any(KeyIntent.class)))
                .thenReturn(Optional.of(jws));
    }

    @Test
    void createChildWorldRejectedIfMain() throws Exception {
        mockMvc.perform(post("/world/user/world")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"worldId\":\"terra\"}")
                .header("Authorization","Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("world must not be main (needs zone or branch)"));
    }

    @Test
    void createChildWorldNeedsOwnership() throws Exception {
        WWorld main = WWorld.builder().worldId("terra").owner(List.of("other"))
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Mockito.when(worldService.getByWorldId("terra")).thenReturn(Optional.of(main));

        mockMvc.perform(post("/world/user/world")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"worldId\":\"terra$zone1\"}")
                .header("Authorization","Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("not an owner of main world"));
    }

    @Test
    void createChildWorldOk() throws Exception {
        WWorld main = WWorld.builder().worldId("terra").owner(List.of(userId)).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Mockito.when(worldService.getByWorldId("terra")).thenReturn(Optional.of(main));
        WWorld child = WWorld.builder().worldId("terra$zone1").parent("terra").owner(List.of(userId)).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Mockito.when(worldService.createWorld(Mockito.eq("terra$zone1"), Mockito.any(), Mockito.eq("terra"), Mockito.any(), Mockito.any()))
                .thenReturn(child);
        Mockito.when(worldService.getByWorldId("terra$zone1")).thenReturn(Optional.of(child));

        mockMvc.perform(post("/world/user/world")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"worldId\":\"terra$zone1\"}")
                .header("Authorization","Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.worldId").value("terra$zone1"))
            .andExpect(jsonPath("$.parent").value("terra"));
    }
}
