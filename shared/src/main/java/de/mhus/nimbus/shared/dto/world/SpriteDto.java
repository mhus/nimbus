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
public class SpriteDto {
    private String id;
    private Integer x;
    private Integer y;
    private Integer z;
    private Integer sizeX;
    private Integer sizeY;
    private Integer sizeZ;
    private List<Long> groups;
    private String reference;
    private Map<String, String> parameters;
    private String rasterType;
    private byte[] raster;
    private String type;
    private Boolean blocking;
    private Integer opacity;
}
