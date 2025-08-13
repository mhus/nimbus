package de.mhus.nimbus.shared.dto.websocket;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterClusterCommandData {
    @JsonProperty("clusters")
    private List<ClusterCoordinate> clusters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterCoordinate {
        @JsonProperty("x")
        private int x;

        @JsonProperty("y")
        private int y;

        @JsonProperty("level")
        private int level;
    }
}
