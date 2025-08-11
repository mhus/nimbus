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
public class AssetDto {
    private String world;
    private String name;
    private String type;
    private byte[] data;
    private Map<String, String> properties;
}
