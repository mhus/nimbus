/*
 * Source TS: ScrawlTypes.ts
 * Original TS: 'interface ScrawlExecContext'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class ScrawlExecContext extends Object {
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private ScrawlSubject actor;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<ScrawlSubject> patients;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.Map<String, Object> vars;
}
