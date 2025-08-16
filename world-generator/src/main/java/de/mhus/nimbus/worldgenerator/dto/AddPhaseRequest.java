package de.mhus.nimbus.worldgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPhaseRequest {
    private String processor;
    private String name;
    private String description;
    private Integer phaseOrder;
    private Map<String, Object> parameters;
}
