/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlScript'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlScript {
    private java.lang.Double schemaVersion;
    private String id;
    private String description;
    private java.util.List<String> imports;
    private java.util.List<ScrawlParameterDefinition> parameters;
    private java.util.Map<String, ScrawlSequence> sequences;
    private ScrawlStep root;
}
