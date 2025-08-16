package de.mhus.nimbus.world.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorldGeneratorRequest {
    private String name;
    private String description;
    private Map<String, Object> parameters;
}
