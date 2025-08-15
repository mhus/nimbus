package de.mhus.nimbus.shared.dto.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("world")
    private String world;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("properties")
    private Map<String, String> properties;
}
