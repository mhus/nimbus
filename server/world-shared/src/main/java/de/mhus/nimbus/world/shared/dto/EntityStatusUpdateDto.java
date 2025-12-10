package de.mhus.nimbus.world.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for entity status updates.
 *
 * Used to broadcast entity status changes (health, death, etc.) across world-player pods.
 * Status fields are dynamic and not predefined - they can contain any key-value pairs.
 *
 * Examples:
 * - Death: {death: 1}
 * - Health: {health: 50, healthMax: 100}
 * - Custom: {mana: 75, stamina: 80}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityStatusUpdateDto {

    /**
     * Entity ID to update.
     * Format: "@userId:characterId"
     */
    private String entityId;

    /**
     * Dynamic status fields.
     * Any key-value pairs representing entity status.
     *
     * Common fields:
     * - "death": Integer (1 = entity died, should be removed)
     * - "health": Number (current health)
     * - "healthMax": Number (maximum health)
     * - "mana": Number
     * - "stamina": Number
     * etc.
     */
    private Map<String, Object> status;
}
