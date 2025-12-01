/*
 * Source TS: ScriptActionDefinition.ts
 * Original TS: 'interface ScriptActionDefinition'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScriptActionDefinition {
    private String scriptId;
    private java.util.Map<String, Object> parameters;
    private ScrawlScript script;
    private java.lang.Boolean sendToServer;
}
