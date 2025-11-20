/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface PlayerBackpack'
 */
package de.mhus.nimbus.generated.configs;

@lombok.Data
@lombok.Builder
public class PlayerBackpack extends Object {
    private java.util.Map<String, String> itemIds;
    private java.util.Map<WEARABLE_SLOT, String> wearingItemIds;
}
