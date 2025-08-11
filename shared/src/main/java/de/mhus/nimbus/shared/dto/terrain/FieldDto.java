package de.mhus.nimbus.shared.dto.terrain;

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
public class FieldDto {
    private Integer x;
    private Integer y;
    private Integer z;
    private List<Long> groups;
    private List<Integer> materials; // index: 0 oben, 1-4: seitliche Materialschichten, 5 unten
    private Integer opacity;
    private Integer sizeZ;
    private Map<String, String> parameters;
}
