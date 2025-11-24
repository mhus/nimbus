/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlParameterDefinition'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlParameterDefinition {
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlParameterType type;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
}
