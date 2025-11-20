/*
 * Source TS: ScriptActionDefinition.ts
 * Original TS: 'interface ScriptActionDefinition'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class ScriptActionDefinition extends Object {
    private String scriptId;
    private java.util.Map<String, Object> parameters;
    private ScrawlScript script;
    private java.lang.Boolean sendToServer;
}
