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
public class WorldDto {
    private String id;
    private Instant createdAt;
    private Instant updatedAt;
    private String name;
    private String description;
    private Map<String, Object> properties;
}
