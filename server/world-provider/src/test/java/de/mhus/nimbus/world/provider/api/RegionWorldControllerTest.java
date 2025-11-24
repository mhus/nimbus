package de.mhus.nimbus.world.provider.api;

import de.mhus.nimbus.generated.types.WorldInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegionWorldControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createMainWorldOk() throws Exception {
        String body = "{\"worldId\":\"terra\",\"info\":{\"name\":\"Terra\"}}"; // minimal WorldInfo JSON (Annahme name-Feld existiert)
        mockMvc.perform(post("/world/region/world")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.worldId").value("terra"));
    }

    @Test
    void rejectNonMainWorld() throws Exception {
        String body = "{\"worldId\":\"terra$eu\",\"info\":{\"name\":\"Terra EU\"}}";
        mockMvc.perform(post("/world/region/world")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("only main worlds can be created (no zone, no branch)"));
    }
}

