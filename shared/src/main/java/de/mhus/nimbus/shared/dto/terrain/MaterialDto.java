package de.mhus.nimbus.shared.dto.terrain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialDto {
    private Integer id;
    private String name;
    private Boolean blocking;
    private Float friction;
    private String color;
    private String texture;
    private String soundWalk;
    private Map<String, String> properties;
}
