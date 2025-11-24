/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface EngineConfiguration'
 */
package de.mhus.nimbus.evaluate.generated.configs;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EngineConfiguration {
    @Deprecated
    @SuppressWarnings("required")
    private de.mhus.nimbus.evaluate.generated.types.WorldInfo worldInfo;
    @Deprecated
    @SuppressWarnings("required")
    private de.mhus.nimbus.evaluate.generated.types.PlayerInfo playerInfo;
    @Deprecated
    @SuppressWarnings("required")
    private PlayerBackpack playerBackpack;
    @Deprecated
    @SuppressWarnings("required")
    private Settings settings;
}
