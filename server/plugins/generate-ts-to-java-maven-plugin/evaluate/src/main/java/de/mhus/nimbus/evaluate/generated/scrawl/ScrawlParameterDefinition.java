/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlParameterDefinition'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlParameterDefinition {
    private String name;
    private ScrawlParameterType type;
    private String description;
}
