package de.mhus.nimbus.world.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldGeneratorPhase {

    private Long id;
    private WorldGenerator worldGenerator;
    private String processor;
    private Integer phaseOrder;
    private String name;
    private Boolean archived;
    private String description;
    private String status; // PENDING, IN_PROGRESS, COMPLETED, ERROR
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> parameters;
}
