package de.mhus.nimbus.region.character;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Repräsentiert ein Item im Inventar oder Ausrüstungs-Slot eines Characters.
 * Anforderungen:
 *  - itemId: eindeutige Referenz (z.B. Datenbank / Katalog)
 *  - name: Anzeige-Name
 *  - texture: Schlüssel oder Pfad für Darstellung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemInfo {
    private String itemId;   // eindeutige ID des Items (Referenz im Katalog)
    private String name;     // Anzeigename
    private String texture;  // Textur-Referenz / Pfad / Key
}
