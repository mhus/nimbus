/*
 * Source TS: ScriptActionDefinition.ts
 * Original TS: 'interface ScriptActionDefinition'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScriptActionDefinition {
    @Deprecated
    @SuppressWarnings("optional")
    private String scriptId;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> parameters;
    @Deprecated
    @SuppressWarnings("optional")
    private ScrawlScript script;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean sendToServer;
}
