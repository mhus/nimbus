/*
 * Source TS: ScrawlTypes.ts
 * Original TS: 'interface ScrawlExecContext'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlExecContext {
    @Deprecated
    @SuppressWarnings("optional")
    private ScrawlSubject actor;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<ScrawlSubject> patients;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> vars;
}
