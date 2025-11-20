/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface Settings'
 */
package de.mhus.nimbus.generated.configs;

@lombok.Data
@lombok.Builder
public class Settings extends Object {
    private String name;
    private String inputController;
    private java.util.Map<String, String> inputMappings;
}
