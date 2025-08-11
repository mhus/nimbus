package de.mhus.nimbus.shared.dto.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerrainFieldDto {
    private Integer x;
    private Integer y;
    private Integer z;
    private List<Long> groups;
    private List<Integer> materials; // 0: top, 1-4: sides, 5: bottom
    private Integer opacity;
    private Integer sizeZ;
    private Map<String, String> parameters;
}
