/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface Settings'
 */
package de.mhus.nimbus.generated.configs;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Settings {
    private String name;
    private String inputController;
    private java.util.Map<String, String> inputMappings;
}
