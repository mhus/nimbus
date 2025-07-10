package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO für Funktionsaufruf-Anfragen von Clients
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallRequest {

    /**
     * Name des Services (z.B. "identity", "registry", "world-voxel", "world-life")
     */
    private String service;

    /**
     * Name der aufzurufenden Methode
     */
    private String method;

    /**
     * Parameter für den Methodenaufruf
     */
    private Object[] parameters;
}
