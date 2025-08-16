package de.mhus.nimbus.world.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldGenerator {

    private Long id;
    private String name;
    private String description;
    private String status; // INITIALIZED, GENERATING, COMPLETED, ERROR
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> parameters;
    private List<WorldGeneratorPhase> phases;
}
