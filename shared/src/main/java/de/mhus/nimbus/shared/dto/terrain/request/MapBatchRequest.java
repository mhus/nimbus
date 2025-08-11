package de.mhus.nimbus.shared.dto.terrain.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapBatchRequest {
    private String world;
    private Integer level;
    private List<ClusterCoordinate> clusters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClusterCoordinate {
        private Integer x;
        private Integer y;
    }
}
