/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface PlayerBackpack'
 */
package de.mhus.nimbus.evaluate.generated.configs;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerBackpack {
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, String> itemIds;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<WEARABLE_SLOT, String> wearingItemIds;
}
