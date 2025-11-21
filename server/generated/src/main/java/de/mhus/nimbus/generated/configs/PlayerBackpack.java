/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface PlayerBackpack'
 */
package de.mhus.nimbus.generated.configs;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerBackpack {
    private java.util.Map<String, String> itemIds;
    private java.util.Map<WEARABLE_SLOT, String> wearingItemIds;
}
