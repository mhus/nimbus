package de.mhus.nimbus.shared.dto.terrain.request;

import de.mhus.nimbus.shared.dto.terrain.ClusterDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapCreateRequest {
    private String world;
    private List<ClusterDto> clusters;
}
