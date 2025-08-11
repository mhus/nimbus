package de.mhus.nimbus.shared.dto.terrain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterDto {
    private Integer level;
    private Integer x;
    private Integer y;
    private List<FieldDto> fields;
}
