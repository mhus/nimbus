package de.mhus.nimbus.shared.dto.terrain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
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
    private Integer rasterMaterial; // Material-ID, alternativ zu rasterData
    private byte[] rasterData; // Darstellung des Sprites als Byte-Array
    private String focusType; // Typ der Focus-Darstellung
    private Integer focusMaterial; // Material-ID für Focus, alternativ zu focusData
    private byte[] focusData; // Focus-Darstellung als Byte-Array
    private String type;
    private Boolean blocking;
    private Integer opacity;
    private Boolean enabled;
}
