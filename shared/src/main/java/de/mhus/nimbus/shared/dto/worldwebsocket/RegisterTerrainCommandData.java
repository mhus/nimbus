package de.mhus.nimbus.shared.dto.worldwebsocket;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTerrainCommandData {
    @JsonProperty("events")
    private List<String> events;
}
