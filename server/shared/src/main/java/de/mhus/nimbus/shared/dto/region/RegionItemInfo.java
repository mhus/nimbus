package de.mhus.nimbus.shared.dto.region;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * DTO für Item-Informationen, genutzt in Character-Backpack und Wearing.
 * Felder:
 *  - itemId: eindeutige Referenz / Katalog-ID
 *  - name: Anzeigename
 *  - texture: Schlüssel oder Pfad zur Textur / Darstellung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionItemInfo {
    private String itemId;
    private String name;
    private String texture;
}
