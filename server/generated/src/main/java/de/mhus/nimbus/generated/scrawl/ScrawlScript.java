package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class ScrawlScript extends Object {
    private java.lang.Double schemaVersion;
    private String id;
    private String description;
    private java.util.List<String> imports;
    private java.util.List<ScrawlParameterDefinition> parameters;
    private java.util.Map<String, ScrawlSequence> sequences;
    private ScrawlStep root;
}
