package de.mhus.nimbus.world.player.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorldInfoControllerTest {

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new WorldInfoController()).build();
    }

    @Test
    void infoEndpointOk() throws Exception {
        mvc.perform(get("/player/world/w1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("world-provider"))
                .andExpect(jsonPath("$.worldId").value("w1"));
    }
}
