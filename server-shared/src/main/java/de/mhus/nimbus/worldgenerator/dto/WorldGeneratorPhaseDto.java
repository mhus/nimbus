package de.mhus.nimbus.worldgenerator.dto;

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
public class WorldGeneratorPhaseDto {
    private Long id;
    private Long worldGeneratorId;
    private String processor;
    private Integer phaseOrder;
    private String name;
    private Boolean archived;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> parameters;
}
