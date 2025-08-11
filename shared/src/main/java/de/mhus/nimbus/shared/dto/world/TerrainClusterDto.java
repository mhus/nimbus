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
public class TerrainClusterDto {
    private Integer level;
    private Integer x;
    private Integer y;
    private List<TerrainFieldDto> fields;
}
