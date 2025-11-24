/*
 * Source TS: EngineConfiguration.ts
 * Original TS: 'interface Settings'
 */
package de.mhus.nimbus.evaluate.generated.configs;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Settings {
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("required")
    private String inputController;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, String> inputMappings;
}
