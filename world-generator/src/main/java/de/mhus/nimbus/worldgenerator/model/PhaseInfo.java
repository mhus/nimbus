package de.mhus.nimbus.worldgenerator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhaseInfo {

    private Long phaseId;
    private Long worldGeneratorId;
    private String processor;
    private String name;
    private String description;
    private Integer phaseOrder;
    private String status;
    private Map<String, Object> parameters;
}
