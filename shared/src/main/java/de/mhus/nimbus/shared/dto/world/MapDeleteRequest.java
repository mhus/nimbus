package de.mhus.nimbus.shared.dto.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapDeleteRequest {
    private String world;
    private Integer level;
    private List<TerrainClusterDeleteDto> clusters;
}
