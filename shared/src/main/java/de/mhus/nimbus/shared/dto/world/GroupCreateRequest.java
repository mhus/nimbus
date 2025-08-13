package de.mhus.nimbus.shared.dto.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateRequest {
    @JsonProperty("world")
    private String world;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("properties")
    private Map<String, String> properties;
}
