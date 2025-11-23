package de.mhus.nimbus.world.provider.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(WorldInfoController.class)
class WorldInfoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void infoEndpointOk() throws Exception {
        mvc.perform(get("/api/world/w1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("world-provider"))
                .andExpect(jsonPath("$.worldId").value("w1"));
    }
}
