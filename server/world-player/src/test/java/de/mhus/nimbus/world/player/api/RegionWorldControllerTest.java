package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Import(RegionWorldControllerTest.MockConfig.class)
class RegionWorldControllerTest {

    @Configuration
    static class MockConfig {
        @Bean WWorldService worldService() { return Mockito.mock(WWorldService.class); }
    }

    @Autowired
    WWorldService worldService;

    MockMvc mockMvc;
    WWorldService localService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        localService = Mockito.mock(WWorldService.class);
        ObjectMapper mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new RegionWorldController(localService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void createMainWorldOk() throws Exception {
        String body = "{\"worldId\":\"terra\",\"info\":{\"name\":\"Terra\"}}";
        WorldInfo info = new WorldInfo();
        info.setName("Terra");
        WWorld created = WWorld.builder().worldId("terra").publicData(info).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Mockito.when(localService.createWorld(Mockito.eq("terra"), Mockito.any(WorldInfo.class))).thenReturn(created);

        mockMvc.perform(post("/world/region/world")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.worldId").value("terra"));
    }

    @Test
    void rejectNonMainWorld() throws Exception {
        String body = "{\"worldId\":\"terra$eu\",\"info\":{\"name\":\"Terra EU\"}}";
        mockMvc.perform(post("/world/region/world")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("only main worlds can be created (no zone, no branch)"));
    }
    @Test
    void getUnknownWorldReturns404() throws Exception {
        mockMvc.perform(get("/world/region/world").param("worldId","terra"))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isNotFound());
    }
}
