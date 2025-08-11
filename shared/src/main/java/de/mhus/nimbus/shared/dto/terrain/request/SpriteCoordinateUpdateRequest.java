package de.mhus.nimbus.shared.dto.terrain.request;

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
}
