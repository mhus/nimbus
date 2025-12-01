/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface Settings'
 */
package de.mhus.nimbus.evaluate.generated.configs;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Settings {
    private String name;
    private String inputController;
    private java.util.Map<String, String> inputMappings;
}
