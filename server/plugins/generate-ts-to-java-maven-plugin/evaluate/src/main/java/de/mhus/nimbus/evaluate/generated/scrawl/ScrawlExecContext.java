/*
 * Source TS: ScrawlTypes.ts
 * Original TS: 'interface ScrawlExecContext'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlExecContext {
    private ScrawlSubject actor;
    private java.util.List<ScrawlSubject> patients;
    private java.util.Map<String, Object> vars;
}
