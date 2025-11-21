/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface EngineConfiguration'
 */
package de.mhus.nimbus.generated.configs;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class EngineConfiguration extends Object {
    private de.mhus.nimbus.generated.types.WorldInfo worldInfo;
    private de.mhus.nimbus.generated.types.PlayerInfo playerInfo;
    private PlayerBackpack playerBackpack;
    private Settings settings;
}
