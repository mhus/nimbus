package de.mhus.nimbus.shared.dto.world;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
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
    private byte[] compressed;
    private Map<String, Object> properties;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant compressedAt;
}
