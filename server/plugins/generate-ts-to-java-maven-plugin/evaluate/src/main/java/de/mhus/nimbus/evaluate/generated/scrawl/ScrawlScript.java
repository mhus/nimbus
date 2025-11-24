/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlScript'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlScript {
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double schemaVersion;
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<String> imports;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<ScrawlParameterDefinition> parameters;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, ScrawlSequence> sequences;
    @Deprecated
    @SuppressWarnings("optional")
    private ScrawlStep root;
}
