/*
 * Source TS: ScrawlTypes.ts
 * Original TS: 'interface ScrawlExecContext'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class ScrawlExecContext extends Object {
    private ScrawlSubject actor;
    private java.util.List<ScrawlSubject> patients;
    private java.util.Map<String, Object> vars;
}
