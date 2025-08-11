package de.mhus.nimbus.shared.dto.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpriteCoordinateUpdateRequest {
    private Integer x;
    private Integer y;
    private Integer z;
    private Integer sizeX;
    private Integer sizeY;
    private Integer sizeZ;
}
