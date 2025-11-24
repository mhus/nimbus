package de.mhus.nimbus.world.provider.session;

import de.mhus.nimbus.world.provider.security.RegionJwtFilter;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionStatus;
import de.mhus.nimbus.world.shared.session.WSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegionSessionControllerTest {

    MockMvc mvc;
    WSessionService sessionService;

    @BeforeEach
    void setup() {
        sessionService = Mockito.mock(WSessionService.class);
        RegionSessionController controller = new RegionSessionController(sessionService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createSessionOk() throws Exception {
        WSession mock = WSession.builder()
                .id("X".repeat(60))
                .status(WSessionStatus.WAITING)
                .worldId("w1")
                .regionId("r1")
                .userId("u1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .expireAt(Instant.now().plus(Duration.ofMinutes(5)))
                .build();
        Mockito.when(sessionService.create("w1","r1","u1", null, null)).thenReturn(mock);
        mvc.perform(post("/world/region/r1/session")
                .param("worldId","w1")
                .param("userId","u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mock.getId()))
                .andExpect(jsonPath("$.regionId").value("r1"))
                .andExpect(jsonPath("$.worldId").value("w1"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }
}
